package com.example.telecom.controller;

import com.example.telecom.error.BadRequestException;
import com.example.telecom.error.ConflictException;
import com.example.telecom.error.NotFoundException;
import com.example.telecom.model.Customer;
import com.example.telecom.model.Device;
import com.example.telecom.repository.CustomerRepository;
import com.example.telecom.repository.DeviceRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/devices")
public class DeviceController {

    private static final Logger LOG = LoggerFactory.getLogger(DeviceController.class);
    // 14 digits without the check digit, 15 with it, or 16 for an IMEISV.
    private static final String IMEI_PATTERN = "^\\d{14,16}$";
    private static final String IMEI_MESSAGE = "must be 14 to 16 digits";
    private static final String SIM_PATTERN = "^\\d{18,22}$";
    private static final String SIM_MESSAGE = "must be 18 to 22 digits";

    private final DeviceRepository repository;
    private final CustomerRepository customerRepository;

    public DeviceController(DeviceRepository repository, CustomerRepository customerRepository) {
        this.repository = repository;
        this.customerRepository = customerRepository;
    }

    @GetMapping
    public List<DeviceResponse> getAllDevices() {
        LOG.info("Fetching all devices");
        List<DeviceResponse> devices = repository.findAll(Sort.by("id")).stream().map(DeviceResponse::from).toList();
        LOG.debug("Found {} devices", devices.size());
        return devices;
    }

    @GetMapping("/{id}")
    public DeviceResponse getDevice(@PathVariable Long id) {
        LOG.info("Fetching device with id={}", id);
        return DeviceResponse.from(findDevice(id));
    }

    @PostMapping
    public DeviceResponse createDevice(@Valid @RequestBody DeviceCreateRequest request) {
        LOG.info("Creating device");
        ensureUnique(request.imei(), request.simNumber(), null);
        Device device = new Device(request.imei(), request.model().trim(), request.simNumber());
        if (request.customerId() != null) {
            assign(device, findCustomer(request.customerId()));
        }
        Device saved = repository.save(device);
        LOG.info("Device created: id={}", saved.getId());
        return DeviceResponse.from(saved);
    }

    /**
     * Partial update. Sending a customerId assigns the device (status becomes
     * ASSIGNED). Setting any status other than ASSIGNED releases the device from
     * its customer.
     */
    @PatchMapping("/{id}")
    public DeviceResponse updateDevice(@PathVariable Long id, @Valid @RequestBody DeviceUpdateRequest request) {
        LOG.info("Updating device: id={}", id);
        Device device = findDevice(id);
        ensureUnique(request.imei(), request.simNumber(), id);
        if (request.imei() != null) {
            device.setImei(request.imei());
        }
        if (request.model() != null) {
            device.setModel(request.model().trim());
        }
        if (request.simNumber() != null) {
            device.setSimNumber(request.simNumber());
        }
        applyAssignment(device, request.status(), request.customerId());
        Device saved = repository.save(device);
        LOG.info("Device updated: id={}", saved.getId());
        return DeviceResponse.from(saved);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDevice(@PathVariable Long id) {
        LOG.info("Deleting device: id={}", id);
        Device device = findDevice(id);
        if (device.getCustomer() != null) {
            throw new ConflictException(
                    "Device " + id + " is assigned to a customer. Unassign it before deleting the record.");
        }
        repository.delete(device);
        LOG.info("Device deleted: id={}", id);
        return ResponseEntity.noContent().build();
    }

    private void applyAssignment(Device device, Device.Status status, Long customerId) {
        if (customerId != null) {
            assignToCustomer(device, status, customerId);
        } else if (status != null) {
            applyStatusWithoutCustomer(device, status);
        }
    }

    private void assignToCustomer(Device device, Device.Status status, Long customerId) {
        if (status != null && status != Device.Status.ASSIGNED) {
            throw new BadRequestException("A device assigned to a customer must have the status ASSIGNED, not " + status
                    + ". Leave customerId empty to release the device.");
        }
        Customer customer = findCustomer(customerId);
        if (device.getCustomer() == null || !device.getCustomer().getId().equals(customer.getId())) {
            assign(device, customer);
        }
    }

    private static void applyStatusWithoutCustomer(Device device, Device.Status status) {
        if (status == Device.Status.ASSIGNED) {
            if (device.getCustomer() == null) {
                throw new BadRequestException("Provide a customerId to assign the device.");
            }
            return;
        }
        device.setStatus(status);
        device.setCustomer(null);
        device.setAssignedAt(null);
    }

    private static void assign(Device device, Customer customer) {
        device.setCustomer(customer);
        device.setStatus(Device.Status.ASSIGNED);
        device.setAssignedAt(Instant.now());
    }

    private void ensureUnique(String imei, String simNumber, Long currentId) {
        if (imei != null) {
            repository.findByImei(imei).filter(other -> !other.getId().equals(currentId)).ifPresent(other -> {
                throw new ConflictException("Another device already uses IMEI " + imei + ".");
            });
        }
        if (simNumber != null) {
            repository.findBySimNumber(simNumber).filter(other -> !other.getId().equals(currentId)).ifPresent(other -> {
                throw new ConflictException("Another device already uses SIM number " + simNumber + ".");
            });
        }
    }

    private Device findDevice(Long id) {
        return repository.findById(id).orElseThrow(() -> new NotFoundException("Device " + id + " does not exist."));
    }

    private Customer findCustomer(Long customerId) {
        return customerRepository.findById(customerId)
                .orElseThrow(() -> new BadRequestException("Customer " + customerId + " does not exist."));
    }

    public record DeviceCreateRequest(@NotBlank @Pattern(regexp = IMEI_PATTERN, message = IMEI_MESSAGE) String imei,
            @NotBlank @Size(max = 100) String model,
            @NotBlank @Pattern(regexp = SIM_PATTERN, message = SIM_MESSAGE) String simNumber, Long customerId) {
    }

    /** Partial update: fields left out or sent as null keep their current value. */
    public record DeviceUpdateRequest(@Pattern(regexp = IMEI_PATTERN, message = IMEI_MESSAGE) String imei,
            @Size(min = 1, max = 100) String model,
            @Pattern(regexp = SIM_PATTERN, message = SIM_MESSAGE) String simNumber, Long customerId,
            Device.Status status) {
    }

    public record DeviceResponse(Long id, String imei, String model, String sim_number, Long customer_id,
            String customer_name, String status, String assigned_at, String created_at) {
        public static DeviceResponse from(Device device) {
            return new DeviceResponse(device.getId(), device.getImei(), device.getModel(), device.getSimNumber(),
                    device.getCustomer() != null ? device.getCustomer().getId() : null,
                    device.getCustomer() != null
                            ? device.getCustomer().getFirstName() + " " + device.getCustomer().getLastName()
                            : null,
                    device.getStatus().name(),
                    device.getAssignedAt() != null ? device.getAssignedAt().toString() : null,
                    device.getCreatedAt() != null ? device.getCreatedAt().toString() : null);
        }
    }
}
