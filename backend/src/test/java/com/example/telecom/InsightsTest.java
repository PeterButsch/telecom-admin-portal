package com.example.telecom;

import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.telecom.dashboard.InsightService;
import com.example.telecom.dashboard.InsightService.Access;
import com.example.telecom.dashboard.InsightService.Alert;
import com.example.telecom.dashboard.InsightService.Area;
import com.example.telecom.dashboard.InsightService.Insights;
import com.example.telecom.dashboard.InsightService.LineItem;
import com.example.telecom.dashboard.InsightService.Severity;
import com.example.telecom.security.StaffRole;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Reads the alerts at 19 February 2026 17:00 UTC, which is already 20 February
 * in Singapore.
 */
class InsightsTest extends IntegrationTestSupport {

    private static final Instant NOW = Instant.parse("2026-02-19T17:00:00Z");
    private static final Access EVERYTHING = new Access(true, true, true, true);

    @Autowired
    private InsightService service;

    private void usage(long lineId, String type, String cost, boolean roaming, String at) {
        jdbc.update("INSERT INTO usage_records (line_id, type, quantity, cost, roaming, recorded_at)"
                + " VALUES (?, ?, 1, ?, ?, ?)", lineId, type, new BigDecimal(cost), roaming ? 1 : 0, at);
    }

    private void line(long id, long planId, String status, String activated) {
        jdbc.update(
                "INSERT INTO mobile_lines (id, account_id, plan_id, msisdn, sim_type, iccid, status, activated_at,"
                        + " created_at) VALUES (?, 1, ?, ?, 'ESIM', ?, ?, ?, ?)",
                id, planId, "9000000" + id, "896501000000000010" + id, status, activated, FIXTURE_TIME);
    }

    private static List<String> ids(Insights insights) {
        return insights.alerts().stream().map(Alert::id).toList();
    }

    private static Alert alert(Insights insights, String id) {
        return insights.alerts().stream().filter(a -> a.id().equals(id)).findFirst().orElseThrow();
    }

    @Test
    void roamingChargesAreCountedOverTheSevenDaysUpToTheLatestUsage() {
        line(4, 1, "ACTIVE", FIXTURE_TIME);
        usage(1, "DATA", "35.00", true, "2026-02-09 00:00:00.000");
        usage(1, "CALL", "15.00", true, "2026-02-10 12:00:00.000");
        usage(1, "DATA", "30.00", false, "2026-02-08 00:00:00.000");
        usage(3, "DATA", "20.00", true, "2026-02-05 00:00:00.000");
        // Exactly 7 days before the latest record: outside the window.
        usage(3, "DATA", "100.00", true, "2026-02-03 12:00:00.000");
        usage(4, "CALL", "19.99", true, "2026-02-06 00:00:00.000");
        // Only active lines are warned about.
        line(5, 1, "SUSPENDED", FIXTURE_TIME);
        usage(5, "DATA", "30.00", true, "2026-02-08 00:00:00.000");

        // Usage stopped nine days before now; the window still ends at the latest
        // record.
        Insights insights = service.insights(NOW, EVERYTHING);

        assertEquals("2026-02-10T12:00:00Z", insights.usage_as_of());
        assertEquals(List.of("roaming-high-1", "roaming-medium"), ids(insights));
        Alert high = alert(insights, "roaming-high-1");
        assertEquals(Severity.HIGH, high.severity());
        assertEquals(Area.USAGE, high.area());
        assertEquals("S$50.00 roaming charges on 9123 4567 in 7 days", high.title());
        assertEquals(List.of(new LineItem(1, "91234567", 1L, "Ada Tan", "Test 5G 100GB", new BigDecimal("50.00"), null,
                "Data S$35.00, calls S$15.00")), high.lines());
        Alert medium = alert(insights, "roaming-medium");
        assertEquals(Severity.MEDIUM, medium.severity());
        assertEquals("1 active line with S$20.00 to S$50.00 roaming charges in 7 days", medium.title());
        assertEquals(1, medium.affected());
        assertEquals("98765432", medium.lines().getFirst().msisdn());
        assertEquals(new BigDecimal("20.00"), medium.lines().getFirst().amount());
        assertEquals("Lion City Logistics Pte. Ltd.", medium.lines().getFirst().customer_name());
    }

    @Test
    void planChecksUseSingaporeDays() {
        jdbc.update("INSERT INTO plans (id, code, name, brand, category, segment, monthly_price, contract_months,"
                + " network, is_active, created_at) VALUES (3, 'TEST-LEGACY', 'Test Legacy 4G', 'SINGTEL', 'POSTPAID',"
                + " 'CONSUMER', 30, 24, '4G', 0, ?)", FIXTURE_TIME);
        jdbc.update("INSERT INTO promotions (id, code, name, discount_type, discount_value, duration_months,"
                + " valid_from) VALUES (1, 'TEST-PROMO', 'Test 20% off', 'PERCENT', 20, 12, '2025-01-01')");
        line(4, 3, "ACTIVE", "2025-06-01 00:00:00.000");
        line(5, 3, "SUSPENDED", "2025-06-01 00:00:00.000");
        // 20 February plus 30 days is 22 March.
        jdbc.update("UPDATE mobile_lines SET contract_end = '2026-03-22', promotion_id = 1,"
                + " promotion_ends_on = '2026-02-20' WHERE id = 1");
        jdbc.update("UPDATE mobile_lines SET contract_end = '2026-03-23', promotion_id = 1,"
                + " promotion_ends_on = '2026-02-19' WHERE id = 3");
        // Still 19 February in UTC, but already over in Singapore.
        jdbc.update("UPDATE mobile_lines SET contract_end = '2026-02-19' WHERE id = 4");
        jdbc.update("UPDATE mobile_lines SET contract_end = '2025-01-01' WHERE id = 2");

        Insights insights = service.insights(NOW, EVERYTHING);

        assertNull(insights.usage_as_of());
        assertEquals(List.of("retired-plan-3", "contracts-ending", "promotions-ending", "out-of-contract"),
                ids(insights));
        Alert retired = alert(insights, "retired-plan-3");
        assertEquals("1 active line on retired plan Test Legacy 4G", retired.title());
        assertTrue(retired.detail().contains("S$30.00 a month"), retired.detail());
        assertEquals(List.of(4L), retired.lines().stream().map(LineItem::line_id).toList());
        assertEquals("2025-06-01T00:00:00Z", retired.lines().getFirst().date());
        Alert ending = alert(insights, "contracts-ending");
        assertEquals("1 contract ends in the next 30 days", ending.title());
        assertEquals(List.of(1L), ending.lines().stream().map(LineItem::line_id).toList());
        assertEquals("2026-03-22", ending.lines().getFirst().date());
        Alert ended = alert(insights, "out-of-contract");
        assertEquals(Severity.LOW, ended.severity());
        assertEquals("1 active line is out of contract", ended.title());
        assertEquals(List.of(4L), ended.lines().stream().map(LineItem::line_id).toList());
        Alert promotions = alert(insights, "promotions-ending");
        assertEquals(List.of(1L), promotions.lines().stream().map(LineItem::line_id).toList());
        assertEquals("Test 20% off", promotions.lines().getFirst().note());
        assertEquals("2026-02-20", promotions.lines().getFirst().date());
    }

    @Test
    void largerGroupsComeFirstWithinASeverity() {
        line(4, 1, "ACTIVE", FIXTURE_TIME);
        jdbc.update("UPDATE mobile_lines SET contract_end = '2026-03-01' WHERE id = 1");
        jdbc.update("UPDATE mobile_lines SET promotion_ends_on = '2026-03-01' WHERE id IN (1, 3, 4)");
        usage(1, "DATA", "60.00", true, FIXTURE_TIME);

        Insights insights = service.insights(NOW, EVERYTHING);

        assertEquals(List.of("roaming-high-1", "promotions-ending", "contracts-ending"), ids(insights));
        assertEquals(3, alert(insights, "promotions-ending").affected());
    }

    @Test
    void nothingToReport() {
        Insights insights = service.insights(NOW, EVERYTHING);

        assertEquals(List.of(), insights.alerts());
        assertNull(insights.usage_as_of());
    }

    @Test
    void eachRoleOnlySeesWhatItMayRead() throws Exception {
        String soon = LocalDate.now(ZoneId.of("Asia/Singapore")).plusDays(5).toString();
        jdbc.update("UPDATE mobile_lines SET contract_end = ? WHERE id = 1", soon);
        usage(1, "DATA", "60.00", true, FIXTURE_TIME);
        String url = "/api/dashboard/insights";

        mockMvc.perform(get(url).with(as(StaffRole.PRODUCT_MANAGER))).andExpect(status().isOk())
                .andExpect(jsonPath("$.usage_as_of").value(nullValue()))
                .andExpect(jsonPath("$.alerts.length()").value(1))
                .andExpect(jsonPath("$.alerts[0].id").value("contracts-ending"))
                .andExpect(jsonPath("$.alerts[0].affected").value(1))
                .andExpect(jsonPath("$.alerts[0].lines").value(nullValue()));
        mockMvc.perform(get(url).with(as(StaffRole.NETWORK_OPS))).andExpect(status().isOk())
                .andExpect(jsonPath("$.usage_as_of").value("2026-02-01T09:00:00Z"))
                .andExpect(jsonPath("$.alerts[0].id").value("roaming-high-1"))
                .andExpect(jsonPath("$.alerts[0].lines[0].msisdn").value("91234567"))
                .andExpect(jsonPath("$.alerts[0].lines[0].customer_name").value(nullValue()))
                .andExpect(jsonPath("$.alerts[1].lines[0].customer_id").value(nullValue()));
        mockMvc.perform(get(url).with(as(StaffRole.CARE_AGENT))).andExpect(status().isOk())
                .andExpect(jsonPath("$.alerts[0].severity").value("HIGH"))
                .andExpect(jsonPath("$.alerts[0].value_kind").value("AMOUNT"))
                .andExpect(jsonPath("$.alerts[0].lines[0].customer_name").value("Ada Tan"))
                .andExpect(jsonPath("$.alerts[1].lines[0].date").value(soon));
        anonymousMvc.perform(get(url)).andExpect(status().isUnauthorized());
    }
}
