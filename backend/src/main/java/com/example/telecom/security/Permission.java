package com.example.telecom.security;

/**
 * What a signed-in staff member may do. Endpoints are mapped to permissions in
 * {@link SecurityConfig}; roles bundle permissions in {@link StaffRole}.
 */
public enum Permission {
    CUSTOMER_READ, CUSTOMER_WRITE, CUSTOMER_DELETE, // customers and accounts
    LINE_READ, CASE_READ, CASE_WRITE, // lines and support cases
    CATALOG_READ, PLAN_WRITE, DEVICE_READ, DEVICE_WRITE, // plans, add-ons and devices
    USAGE_READ, DASHBOARD_READ, // usage records and figures
    AUDIT_READ, STAFF_ADMIN // oversight and administration
}
