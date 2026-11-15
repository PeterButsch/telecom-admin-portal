package com.example.telecom.audit;

/** Everything the audit trail records, with the kind of record it is about. */
public enum AuditAction {
    LOGIN(Entity.SESSION), LOGIN_FAILED(Entity.SESSION), LOGOUT(Entity.SESSION), ACCESS_DENIED(Entity.ENDPOINT),
    PASSWORD_CHANGED(Entity.STAFF_USER), ACCOUNT_LOCKED(Entity.STAFF_USER), STAFF_USER_CREATED(Entity.STAFF_USER),
    STAFF_USER_UPDATED(Entity.STAFF_USER), STAFF_USER_UNLOCKED(Entity.STAFF_USER),
    STAFF_PASSWORD_RESET(Entity.STAFF_USER), CUSTOMER_VIEWED(Entity.CUSTOMER), CUSTOMER_CREATED(Entity.CUSTOMER),
    CUSTOMER_UPDATED(Entity.CUSTOMER), CUSTOMER_DELETED(Entity.CUSTOMER), ACCOUNT_CREATED(Entity.ACCOUNT),
    CASE_CREATED(Entity.CASE), CASE_UPDATED(Entity.CASE), PLAN_CREATED(Entity.PLAN), PLAN_UPDATED(Entity.PLAN),
    PLAN_DELETED(Entity.PLAN), DEVICE_CREATED(Entity.DEVICE), DEVICE_UPDATED(Entity.DEVICE),
    DEVICE_DELETED(Entity.DEVICE);

    /** Record types that audit events refer to. */
    public enum Entity {
        SESSION, ENDPOINT, STAFF_USER, CUSTOMER, ACCOUNT, CASE, PLAN, DEVICE
    }

    private final Entity entityType;

    AuditAction(Entity entityType) {
        this.entityType = entityType;
    }

    public Entity entity() {
        return entityType;
    }
}
