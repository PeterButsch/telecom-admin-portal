package com.example.telecom.controller;

import com.example.telecom.api.Times;
import com.example.telecom.dashboard.DashboardOverviewService;
import com.example.telecom.dashboard.DashboardOverviewService.Overview;
import com.example.telecom.model.Customer;
import com.example.telecom.model.Device;
import com.example.telecom.model.MobileLine;
import com.example.telecom.model.SupportCase;
import com.example.telecom.repository.CustomerRepository;
import com.example.telecom.repository.DeviceRepository;
import com.example.telecom.repository.MobileLineRepository;
import com.example.telecom.repository.SupportCaseRepository;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Headline figures and chart data. Monthly recurring revenue is the list price
 * of plans on active lines.
 */
@RestController
@RequestMapping("/api/dashboard")
@Transactional(readOnly = true)
public class DashboardController {

    private static final List<SupportCase.Status> OPEN_STATUSES = List.of(SupportCase.Status.OPEN,
            SupportCase.Status.IN_PROGRESS, SupportCase.Status.PENDING_CUSTOMER);

    private final CustomerRepository customerRepository;
    private final MobileLineRepository lineRepository;
    private final DeviceRepository deviceRepository;
    private final SupportCaseRepository caseRepository;
    private final DashboardOverviewService overviewService;

    public DashboardController(CustomerRepository customerRepository, MobileLineRepository lineRepository,
            DeviceRepository deviceRepository, SupportCaseRepository caseRepository,
            DashboardOverviewService overviewService) {
        this.customerRepository = customerRepository;
        this.lineRepository = lineRepository;
        this.deviceRepository = deviceRepository;
        this.caseRepository = caseRepository;
        this.overviewService = overviewService;
    }

    /**
     * Revenue and ARPU by brand, consumer versus enterprise, monthly line movement,
     * churn, porting and case SLAs. See {@link DashboardOverviewService} for the
     * definitions.
     */
    @GetMapping("/overview")
    public Overview getOverview() {
        return overviewService.overview(Times.now());
    }

    @GetMapping("/stats")
    public DashboardStats getStats() {
        BigDecimal revenue = lineRepository.sumActiveRevenueByPlanName().stream()
                .map(row -> CustomerController.toMoney(row[1])).reduce(BigDecimal.ZERO.setScale(2), BigDecimal::add);
        return new DashboardStats(customerRepository.countByStatus(Customer.Status.ACTIVE),
                lineRepository.countByStatus(MobileLine.Status.ACTIVE), revenue,
                caseRepository.countByStatusIn(OPEN_STATUSES), deviceRepository.countByStatus(Device.Status.ASSIGNED));
    }

    @GetMapping("/lines-by-plan")
    public List<ChartData> getLinesByPlan() {
        return lineRepository.countActiveByPlanName().stream()
                .map(row -> new ChartData((String) row[0], ((Number) row[1]).longValue())).toList();
    }

    @GetMapping("/devices-by-status")
    public List<ChartData> getDevicesByStatus() {
        return deviceRepository.countGroupedByStatus().stream()
                .map(row -> new ChartData(((Device.Status) row[0]).name(), ((Number) row[1]).longValue())).toList();
    }

    @GetMapping("/cases-by-status")
    public List<ChartData> getCasesByStatus() {
        return caseRepository.countGroupedByStatus().stream()
                .map(row -> new ChartData(((SupportCase.Status) row[0]).name(), ((Number) row[1]).longValue()))
                .toList();
    }

    @GetMapping("/revenue-by-plan")
    public List<RevenueData> getRevenueByPlan() {
        return lineRepository.sumActiveRevenueByPlanName().stream()
                .map(row -> new RevenueData((String) row[0], CustomerController.toMoney(row[1]))).toList();
    }

    public record DashboardStats(long active_customers, long active_lines, BigDecimal monthly_revenue, long open_cases,
            long devices_in_use) {
    }

    public record ChartData(String name, long value) {
    }

    public record RevenueData(String name, BigDecimal revenue) {
    }
}
