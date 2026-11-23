package com.example.telecom.security;

import static com.example.telecom.security.Permission.AUDIT_READ;
import static com.example.telecom.security.Permission.CASE_READ;
import static com.example.telecom.security.Permission.CASE_WRITE;
import static com.example.telecom.security.Permission.CATALOG_READ;
import static com.example.telecom.security.Permission.CUSTOMER_DELETE;
import static com.example.telecom.security.Permission.CUSTOMER_READ;
import static com.example.telecom.security.Permission.CUSTOMER_WRITE;
import static com.example.telecom.security.Permission.DASHBOARD_READ;
import static com.example.telecom.security.Permission.DEVICE_READ;
import static com.example.telecom.security.Permission.DEVICE_WRITE;
import static com.example.telecom.security.Permission.LINE_READ;
import static com.example.telecom.security.Permission.PLAN_WRITE;
import static com.example.telecom.security.Permission.USAGE_READ;

import java.util.EnumSet;
import java.util.Set;

/**
 * Staff roles. Each role only gets the permissions its job needs; for example
 * product managers see the catalogue and dashboard but no customer records.
 */
@SuppressWarnings("PMD.TooManyStaticImports")
public enum StaffRole {
    /** Front-line care: serves customers, their lines, cases and devices. */
    CARE_AGENT("Care Agent", EnumSet.of(CUSTOMER_READ, CUSTOMER_WRITE, LINE_READ, CASE_READ, CASE_WRITE, CATALOG_READ,
            DEVICE_READ, DEVICE_WRITE, USAGE_READ, DASHBOARD_READ)),
    /** A care agent who may also delete customers and read the audit log. */
    CARE_TEAM_LEAD("Care Team Lead", EnumSet.of(CUSTOMER_READ, CUSTOMER_WRITE, CUSTOMER_DELETE, LINE_READ, CASE_READ,
            CASE_WRITE, CATALOG_READ, DEVICE_READ, DEVICE_WRITE, USAGE_READ, DASHBOARD_READ, AUDIT_READ)),
    /** Investigates scams and misuse: reads records and the audit trail. */
    FRAUD_ANALYST("Fraud & Compliance Analyst", EnumSet.of(CUSTOMER_READ, LINE_READ, CASE_READ, CASE_WRITE,
            CATALOG_READ, DEVICE_READ, USAGE_READ, DASHBOARD_READ, AUDIT_READ)),
    /** Looks after lines, usage and network cases, without customer records. */
    NETWORK_OPS("Network Operations",
            EnumSet.of(LINE_READ, CASE_READ, CATALOG_READ, DEVICE_READ, USAGE_READ, DASHBOARD_READ)),
    /** Owns the plan catalogue. */
    PRODUCT_MANAGER("Product Manager", EnumSet.of(CATALOG_READ, PLAN_WRITE, DEVICE_READ, DASHBOARD_READ)),
    /** Everything, including staff accounts. */
    ADMIN("Administrator", EnumSet.allOf(Permission.class));

    private final String displayLabel;
    private final Set<Permission> granted;

    StaffRole(String displayLabel, Set<Permission> granted) {
        this.displayLabel = displayLabel;
        this.granted = EnumSet.copyOf(granted);
    }

    public String label() {
        return displayLabel;
    }

    public Set<Permission> permissions() {
        return Set.copyOf(granted);
    }

    public boolean has(Permission permission) {
        return granted.contains(permission);
    }
}
