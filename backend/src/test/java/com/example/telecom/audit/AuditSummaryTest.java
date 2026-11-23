package com.example.telecom.audit;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class AuditSummaryTest {

    @Test
    void summariesNeverContainFullIdNumbers() {
        assertEquals("Checked *****567D and *****321H", AuditService.clean("Checked S1234567D and t7654321h", 200));
        assertEquals("one line", AuditService.clean("one\r\nline", 200));
        assertEquals("abc", AuditService.clean("abcdef", 3));
        assertEquals("-", AuditService.clean(null, 10));
    }

    @Test
    void changeSetsListPersonalFieldsWithoutValues() {
        ChangeSet changes = new ChangeSet().field("email", "a@example.com", "b@example.com").field("phone", "1", "1")
                .value("price", new BigDecimal("40.00"), new BigDecimal("45")).value("limit", 100, null)
                .value("same", new BigDecimal("1.0"), new BigDecimal("1.00"));
        assertEquals("email, price 40 → 45, limit 100 → none", changes.describe());
        assertEquals("no changes", new ChangeSet().describe());
    }
}
