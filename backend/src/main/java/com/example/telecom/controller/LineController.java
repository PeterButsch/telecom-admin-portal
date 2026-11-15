package com.example.telecom.controller;

import com.example.telecom.api.PageResponse;
import com.example.telecom.api.Paging;
import com.example.telecom.api.SearchTerms;
import com.example.telecom.api.Times;
import com.example.telecom.error.NotFoundException;
import com.example.telecom.model.Customer;
import com.example.telecom.model.Device;
import com.example.telecom.model.LineAddOn;
import com.example.telecom.model.MobileLine;
import com.example.telecom.model.Plan;
import com.example.telecom.repository.DeviceRepository;
import com.example.telecom.repository.LineAddOnRepository;
import com.example.telecom.repository.MobileLineRepository;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Mobile lines (subscriptions). Line changes such as plan changes and SIM swaps
 * are separate actions.
 */
@RestController
@RequestMapping("/api/lines")
public class LineController {

    private final MobileLineRepository repository;
    private final LineAddOnRepository addOnRepository;
    private final DeviceRepository deviceRepository;

    public LineController(MobileLineRepository repository, LineAddOnRepository addOnRepository,
            DeviceRepository deviceRepository) {
        this.repository = repository;
        this.addOnRepository = addOnRepository;
        this.deviceRepository = deviceRepository;
    }

    /**
     * {@code q} matches the MSISDN, ICCID, account number, customer name or line
     * user.
     */
    @GetMapping
    @Transactional(readOnly = true)
    public PageResponse<LineResponse> getLines(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size, @RequestParam(required = false) String q,
            @RequestParam(required = false) MobileLine.Status status, @RequestParam(required = false) Plan.Brand brand,
            @RequestParam(required = false) Long customerId, @RequestParam(required = false) Long accountId) {
        SearchTerms terms = SearchTerms.of(q);
        return PageResponse.of(repository.search(status, brand, customerId, accountId, terms.text(), terms.digits(),
                Paging.of(page, size)), LineResponse::from);
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public LineDetailResponse getLine(@PathVariable Long id) {
        MobileLine line = repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Line " + id + " does not exist."));
        List<AddOnResponse> addOns = addOnRepository.findByLineIdAndEndedAtIsNullOrderById(id).stream()
                .map(AddOnResponse::from).toList();
        DeviceSummary device = deviceRepository.findFirstByLineIdAndStatus(id, Device.Status.ASSIGNED)
                .map(DeviceSummary::from).orElse(null);
        return new LineDetailResponse(LineResponse.from(line), addOns, device);
    }

    public record LineResponse(Long id, String msisdn, String sim_type, String iccid, String status,
            String status_reason, String brand, Long plan_id, String plan_name, String plan_category,
            BigDecimal monthly_price, Long account_id, String account_number, String account_type, Long customer_id,
            String customer_name, String customer_type, String user_name, String promotion_code,
            String promotion_ends_on, String contract_start, String contract_end, String port_in_from,
            String port_out_to, String activated_at, String terminated_at) {

        static LineResponse from(MobileLine l) {
            Plan plan = l.getPlan();
            Customer customer = l.getAccount().getCustomer();
            return new LineResponse(l.getId(), l.getMsisdn(), l.getSimType().name(), l.getIccid(), l.getStatus().name(),
                    l.getStatusReason(), plan.getBrand().name(), plan.getId(), plan.getName(),
                    plan.getCategory().name(), CustomerController.toMoney(plan.getMonthlyPrice()),
                    l.getAccount().getId(), l.getAccount().getAccountNumber(), l.getAccount().getType().name(),
                    customer.getId(), customer.displayName(), customer.getType().name(), l.getUserName(),
                    l.getPromotion() != null ? l.getPromotion().getCode() : null, Times.iso(l.getPromotionEndsOn()),
                    Times.iso(l.getContractStart()), Times.iso(l.getContractEnd()), name(l.getPortInFrom()),
                    name(l.getPortOutTo()), Times.iso(l.getActivatedAt()), Times.iso(l.getTerminatedAt()));
        }

        private static String name(Enum<?> value) {
            return value == null ? null : value.name();
        }
    }

    public record AddOnResponse(Long id, String code, String name, String category, BigDecimal monthly_price,
            String started_at) {
        static AddOnResponse from(LineAddOn a) {
            return new AddOnResponse(a.getAddOn().getId(), a.getAddOn().getCode(), a.getAddOn().getName(),
                    a.getAddOn().getCategory().name(), CustomerController.toMoney(a.getAddOn().getMonthlyPrice()),
                    Times.iso(a.getStartedAt()));
        }
    }

    public record DeviceSummary(Long id, String imei, String manufacturer, String model) {
        static DeviceSummary from(Device d) {
            return new DeviceSummary(d.getId(), d.getImei(), d.getManufacturer(), d.getModel());
        }
    }

    public record LineDetailResponse(LineResponse line, List<AddOnResponse> add_ons, DeviceSummary device) {
        public LineDetailResponse {
            add_ons = List.copyOf(add_ons);
        }
    }
}
