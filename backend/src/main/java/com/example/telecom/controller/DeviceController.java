package com.example.telecom.controller;

import com.example.telecom.api.Times;
import com.example.telecom.audit.AuditAction;
import com.example.telecom.audit.AuditService;
import com.example.telecom.audit.ChangeSet;
import com.example.telecom.error.BadRequestException;
import com.example.telecom.error.ConflictException;
import com.example.telecom.error.FieldValidationException;
import com.example.telecom.error.NotFoundException;
import com.example.telecom.model.Device;
import com.example.telecom.model.MobileLine;
import com.example.telecom.repository.DeviceRepository;
import com.example.telecom.repository.MobileLineRepository;
import com.example.telecom.security.Permission;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Handset inventory. A device is either in stock or assigned to one mobile line
 * (identified by MSISDN).
 */
@RestController
@RequestMapping("/api/devices")
public class DeviceController {

    private static final Logger LOG = LoggerFactory.getLogger(DeviceController.class);
    // 14 digits without the check digit, 15 with it, or 16 for an IMEISV.
    private static final String IMEI_PATTERN = "^\\d{14,16}$";
    private static final String IMEI_MESSAGE = "must be 14 to 16 digits";
    static final String MSISDN_PATTERN = "^(\\+?65)?[ -]?[89]\\d{3}[ -]?\\d{4}$";
    static final String MSISDN_MESSAGE = "must be a Singapore mobile number such as 9123 4567";

    private final DeviceRepository repository;
    private final MobileLineRepository lineRepository;
    private final AuditService audit;

    public DeviceController(DeviceRepository repository, MobileLineRepository lineRepository, AuditService audit) {
        this.repository = repository;
        this.lineRepository = lineRepository;
        this.audit = audit;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public List<DeviceResponse> getAllDevices() {
        boolean lines = Permission.LINE_READ.isGranted();
        boolean customers = Permission.CUSTOMER_READ.isGranted();
        return repository.findAll(Sort.by("id")).stream().map(d -> DeviceResponse.from(d, lines, customers)).toList();
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public DeviceResponse getDevice(@PathVariable Long id) {
        return response(findDevice(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public DeviceResponse createDevice(@Valid @RequestBody DeviceCreateRequest request) {
        ensureUniqueImei(request.imei(), null);
        Device device = new Device();
        device.setImei(request.imei());
        device.setManufacturer(request.manufacturer().trim());
        device.setModel(request.model().trim());
        if (request.msisdn() != null) {
            assign(device, findAssignableLine(request.msisdn(), null));
        }
        Device saved = repository.save(device);
        LOG.info("Device created: id={}", saved.getId());
        audit.record(AuditAction.DEVICE_CREATED, saved.getId(), "Added " + saved.getManufacturer() + " "
                + saved.getModel() + " IMEI " + saved.getImei() + assignment(saved));
        return response(saved);
    }

    /**
     * Partial update. Sending an msisdn assigns the device to that line (status
     * becomes ASSIGNED). Setting any other status releases the device from its
     * line.
     */
    @PatchMapping("/{id}")
    @Transactional
    public DeviceResponse updateDevice(@PathVariable Long id, @Valid @RequestBody DeviceUpdateRequest request) {
        Device device = findDevice(id);
        ensureUniqueImei(request.imei(), id);
        String imei = device.getImei();
        String model = device.getManufacturer() + " " + device.getModel();
        Device.Status status = device.getStatus();
        String msisdn = device.getLine() != null ? device.getLine().getMsisdn() : null;
        if (request.imei() != null) {
            device.setImei(request.imei());
        }
        if (request.manufacturer() != null) {
            device.setManufacturer(request.manufacturer().trim());
        }
        if (request.model() != null) {
            device.setModel(request.model().trim());
        }
        applyAssignment(device, request);
        Device saved = repository.save(device);
        ChangeSet changes = new ChangeSet().value("IMEI", imei, saved.getImei())
                .value("model", model, saved.getManufacturer() + " " + saved.getModel())
                .value("status", status, saved.getStatus())
                .value("line", msisdn, saved.getLine() != null ? saved.getLine().getMsisdn() : null);
        if (!changes.isEmpty()) {
            audit.record(AuditAction.DEVICE_UPDATED, id, "Updated device " + imei + ": " + changes.describe());
        }
        return response(saved);
    }

    private void applyAssignment(Device device, DeviceUpdateRequest request) {
        Device.Status status = request.status();
        if (request.msisdn() != null) {
            assignTo(device, request.msisdn(), status);
        } else if (status == Device.Status.ASSIGNED && device.getLine() == null) {
            throw new BadRequestException("Provide an msisdn to assign the device.");
        } else if (status != null && status != Device.Status.ASSIGNED) {
            device.setStatus(status);
            device.setLine(null);
            device.setAssignedAt(null);
        }
    }

    private void assignTo(Device device, String msisdn, Device.Status status) {
        if (status != null && status != Device.Status.ASSIGNED) {
            throw new BadRequestException("A device assigned to a line must have the status ASSIGNED, not " + status
                    + ". Leave msisdn empty to release the device.");
        }
        MobileLine line = findAssignableLine(msisdn, device.getId());
        if (device.getLine() == null || !device.getLine().getId().equals(line.getId())) {
            assign(device, line);
        }
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<Void> deleteDevice(@PathVariable Long id) {
        Device device = findDevice(id);
        if (device.getLine() != null) {
            throw new ConflictException("Device " + id + " is assigned to a line. Release it before deleting it.");
        }
        repository.delete(device);
        audit.record(AuditAction.DEVICE_DELETED, id, "Deleted device " + device.getImei());
        return ResponseEntity.noContent().build();
    }

    private MobileLine findAssignableLine(String msisdn, Long deviceId) {
        String number = normalizeMsisdn(msisdn);
        MobileLine line = lineRepository.findByMsisdn(number)
                .orElseThrow(() -> new BadRequestException("No line has the number " + number + "."));
        if (line.getStatus() == MobileLine.Status.TERMINATED) {
            throw new BadRequestException("Line " + number + " is terminated; devices cannot be assigned to it.");
        }
        repository.findFirstByLineIdAndStatus(line.getId(), Device.Status.ASSIGNED)
                .filter(other -> !other.getId().equals(deviceId)).ifPresent(other -> {
                    throw new ConflictException("Line " + number + " already has device " + other.getImei()
                            + ". Release that device first.");
                });
        return line;
    }

    /**
     * Validates and normalizes an optional MSISDN request parameter; null when it
     * is blank.
     */
    static String parseMsisdn(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        if (!value.trim().matches(MSISDN_PATTERN)) {
            throw new FieldValidationException("msisdn", MSISDN_MESSAGE);
        }
        return normalizeMsisdn(value);
    }

    /** Reduces "+65 9123 4567" and similar forms to the 8-digit MSISDN. */
    static String normalizeMsisdn(String value) {
        String digits = value.replaceAll("\\D", "");
        return digits.length() > 8 && digits.startsWith("65") ? digits.substring(2) : digits;
    }

    private static String assignment(Device device) {
        return device.getLine() != null ? ", assigned to " + device.getLine().getMsisdn() : "";
    }

    private static void assign(Device device, MobileLine line) {
        device.setLine(line);
        device.setStatus(Device.Status.ASSIGNED);
        device.setAssignedAt(Times.now());
    }

    private void ensureUniqueImei(String imei, Long currentId) {
        if (imei != null) {
            repository.findByImei(imei).filter(other -> !other.getId().equals(currentId)).ifPresent(other -> {
                throw new ConflictException("Another device already uses IMEI " + imei + ".");
            });
        }
    }

    private static DeviceResponse response(Device device) {
        return DeviceResponse.from(device, Permission.LINE_READ.isGranted(), Permission.CUSTOMER_READ.isGranted());
    }

    private Device findDevice(Long id) {
        return repository.findById(id).orElseThrow(() -> new NotFoundException("Device " + id + " does not exist."));
    }

    public record DeviceCreateRequest(@NotBlank @Pattern(regexp = IMEI_PATTERN, message = IMEI_MESSAGE) String imei,
            @NotBlank @Size(max = 50) String manufacturer, @NotBlank @Size(max = 100) String model,
            @Pattern(regexp = MSISDN_PATTERN, message = MSISDN_MESSAGE) String msisdn) {
    }

    /** Partial update: fields left out or sent as null keep their current value. */
    public record DeviceUpdateRequest(@Pattern(regexp = IMEI_PATTERN, message = IMEI_MESSAGE) String imei,
            @Size(min = 1, max = 50) String manufacturer, @Size(min = 1, max = 100) String model,
            @Pattern(regexp = MSISDN_PATTERN, message = MSISDN_MESSAGE) String msisdn, Device.Status status) {
    }

    public record DeviceResponse(Long id, String imei, String manufacturer, String model, String status, Long line_id,
            String msisdn, Long customer_id, String customer_name, String assigned_at, String created_at) {
        /**
         * The line is left out without {@code showLine}, the customer without
         * {@code showCustomer}.
         */
        static DeviceResponse from(Device d, boolean showLine, boolean showCustomer) {
            MobileLine line = showLine ? d.getLine() : null;
            MobileLine customerLine = showCustomer ? d.getLine() : null;
            return new DeviceResponse(d.getId(), d.getImei(), d.getManufacturer(), d.getModel(), d.getStatus().name(),
                    line != null ? line.getId() : null, line != null ? line.getMsisdn() : null,
                    customerLine != null ? customerLine.getAccount().getCustomer().getId() : null,
                    customerLine != null ? customerLine.getAccount().getCustomer().displayName() : null,
                    Times.iso(d.getAssignedAt()), Times.iso(d.getCreatedAt()));
        }
    }
}
