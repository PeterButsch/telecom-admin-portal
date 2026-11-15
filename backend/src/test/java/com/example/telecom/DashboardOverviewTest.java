package com.example.telecom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.telecom.dashboard.DashboardOverviewService;
import com.example.telecom.dashboard.DashboardOverviewService.BrandFigures;
import com.example.telecom.dashboard.DashboardOverviewService.CasePriorityFigures;
import com.example.telecom.dashboard.DashboardOverviewService.Kpis;
import com.example.telecom.dashboard.DashboardOverviewService.MonthFigures;
import com.example.telecom.dashboard.DashboardOverviewService.OperatorFigures;
import com.example.telecom.dashboard.DashboardOverviewService.Overview;
import com.example.telecom.dashboard.DashboardOverviewService.SegmentFigures;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Adds lines either side of the Singapore month boundary (1 February 00:00 in
 * Singapore is 31 January 16:00 UTC), ported lines and cases in every state to
 * the fixture, then reads the overview as at 20 February 2026 00:00 UTC.
 */
class DashboardOverviewTest extends IntegrationTestSupport {

    private static final Instant NOW = Instant.parse("2026-02-20T00:00:00Z");

    @Autowired
    private DashboardOverviewService service;

    private void line(long id, long planId, String status, String activated, String terminated, String portIn,
            String portOut) {
        jdbc.update(
                "INSERT INTO mobile_lines (id, account_id, plan_id, msisdn, sim_type, iccid, status,"
                        + " activated_at, terminated_at, port_in_from, port_out_to, created_at)"
                        + " VALUES (?, 1, ?, ?, 'ESIM', ?, ?, ?, ?, ?, ?, ?)",
                id, planId, "9000000" + id, "896501000000000010" + id, status, activated, terminated, portIn, portOut,
                FIXTURE_TIME);
    }

    private void supportCase(long id, String priority, String status, String due, String resolved) {
        jdbc.update("INSERT INTO support_cases (id, case_number, customer_id, category, channel, subject, priority,"
                + " status, sla_due_at, created_at, resolved_at) VALUES (?, ?, 1, 'NETWORK', 'APP', 'No signal', ?, ?,"
                + " ?, '2026-01-01 00:00:00.000', ?)", id, "CS100000" + id, priority, status, due, resolved);
    }

    @BeforeEach
    void addMovement() {
        jdbc.update("UPDATE mobile_lines SET port_in_from = 'M1' WHERE id = 1");
        jdbc.update("UPDATE mobile_lines SET port_out_to = 'STARHUB' WHERE id = 2");
        // 1 February 00:30 in Singapore: counts in February.
        line(4, 2, "ACTIVE", "2026-01-31 16:30:00.000", null, null, null);
        line(5, 1, "TERMINATED", "2026-01-10 00:00:00.000", "2026-02-10 00:00:00.000", null, "M1");
        line(6, 2, "ACTIVE", "2026-01-05 00:00:00.000", null, "CIRCLES_LIFE", null);
        // 31 January 23:30 in Singapore: counts in January.
        line(7, 1, "ACTIVE", "2026-01-31 15:30:00.000", null, null, null);
        supportCase(1, "URGENT", "OPEN", "2026-02-19 00:00:00.000", null);
        supportCase(2, "URGENT", "IN_PROGRESS", "2026-02-21 00:00:00.000", null);
        supportCase(3, "LOW", "PENDING_CUSTOMER", "2026-02-25 00:00:00.000", null);
        supportCase(4, "HIGH", "RESOLVED", "2026-02-11 00:00:00.000", "2026-02-10 00:00:00.000");
        supportCase(5, "MEDIUM", "CLOSED", "2026-02-11 00:00:00.000", "2026-02-12 00:00:00.000");
        supportCase(6, "LOW", "RESOLVED", "2026-01-02 00:00:00.000", "2026-01-01 00:00:00.000");
    }

    private static BigDecimal money(String value) {
        return new BigDecimal(value);
    }

    @Test
    void revenueAndArpuCountActiveLinesByBrandAndSegment() {
        Overview overview = service.overview(NOW);

        assertEquals(List.of(new BrandFigures("SINGTEL", 3, money("120.00"), money("40.00")),
                new BrandFigures("GOMO", 0, money("0.00"), null),
                new BrandFigures("HI_CARD", 2, money("36.00"), money("18.00"))), overview.brands());
        assertEquals(List.of(new SegmentFigures("CONSUMER", 1, 4, money("116.00"), money("29.00")),
                new SegmentFigures("ENTERPRISE", 1, 1, money("40.00"), money("40.00"))), overview.segments());
        Kpis kpis = overview.kpis();
        assertEquals(2, kpis.active_customers());
        assertEquals(5, kpis.active_lines());
        assertEquals(money("156.00"), kpis.monthly_revenue());
        assertEquals(money("31.20"), kpis.arpu());
    }

    @Test
    void monthsFollowTheSingaporeCalendar() {
        List<MonthFigures> months = service.overview(NOW).months();

        assertEquals(12, months.size());
        assertEquals("2025-03", months.getFirst().month());
        MonthFigures january = months.get(10);
        MonthFigures february = months.get(11);
        assertEquals(new MonthFigures("2026-01", false, 0, 3, 0, 1, 0, null), january);
        assertEquals(new MonthFigures("2026-02", true, 3, 4, 2, 1, 2, money("66.67")), february);
        assertTrue(months.subList(0, 10).stream().allMatch(m -> m.activations() == 0 && m.lines_at_start() == 0));
        assertFalse(months.get(9).partial());
    }

    @Test
    void recentFiguresCoverTheLastThirtyDays() {
        Kpis kpis = service.overview(NOW).kpis();

        // Window starts 21 January: lines 5 and 6 were already in service, and both
        // lines 2 and 5 ended.
        assertEquals(5, kpis.activations_30d());
        assertEquals(2, kpis.terminations_30d());
        assertEquals(3, kpis.net_adds_30d());
        assertEquals(money("100.00"), kpis.churn_rate_30d());
        assertEquals(1, kpis.port_ins_30d());
        assertEquals(2, kpis.port_outs_30d());
    }

    @Test
    void portingIsGroupedByOtherOperator() {
        assertEquals(List.of(new OperatorFigures("M1", 1, 1), new OperatorFigures("STARHUB", 0, 1),
                new OperatorFigures("SIMBA", 0, 0), new OperatorFigures("CIRCLES_LIFE", 1, 0),
                new OperatorFigures("OTHER", 0, 0)), service.overview(NOW).porting());
    }

    @Test
    void openCasesAndSlaPerformance() {
        Overview overview = service.overview(NOW);

        assertEquals(
                List.of(new CasePriorityFigures("URGENT", 2, 1), new CasePriorityFigures("HIGH", 0, 0),
                        new CasePriorityFigures("MEDIUM", 0, 0), new CasePriorityFigures("LOW", 1, 0)),
                overview.open_cases());
        assertEquals(3, overview.kpis().open_cases());
        assertEquals(1, overview.kpis().breached_open_cases());
        assertEquals(2, overview.kpis().resolved_30d());
        assertEquals(money("50.00"), overview.kpis().sla_met_rate_30d());
    }

    @Test
    void emptyPeriodsHaveNoRates() {
        Overview overview = service.overview(Instant.parse("2025-06-01T00:00:00Z"));

        assertNull(overview.kpis().churn_rate_30d());
        assertNull(overview.kpis().sla_met_rate_30d());
        assertEquals(0, overview.kpis().activations_30d());
    }

    @Test
    void endpointRequiresSignIn() throws Exception {
        mockMvc.perform(get("/api/dashboard/overview")).andExpect(status().isOk())
                .andExpect(jsonPath("$.brands.length()").value(3)).andExpect(jsonPath("$.months.length()").value(12))
                .andExpect(jsonPath("$.kpis.active_lines").value(5)).andExpect(jsonPath("$.as_of").isNotEmpty());
        anonymousMvc.perform(get("/api/dashboard/overview")).andExpect(status().isUnauthorized());
    }
}
