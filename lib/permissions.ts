/** What a signed-in staff member may do; mirrors the backend's Permission enum. */
export type Permission =
  | "CUSTOMER_READ"
  | "CUSTOMER_WRITE"
  | "CUSTOMER_DELETE"
  | "LINE_READ"
  | "CASE_READ"
  | "CASE_WRITE"
  | "CATALOG_READ"
  | "PLAN_WRITE"
  | "DEVICE_READ"
  | "DEVICE_WRITE"
  | "USAGE_READ"
  | "DASHBOARD_READ"
  | "AUDIT_READ"
  | "STAFF_ADMIN"

export const STAFF_ROLES = [
  "CARE_AGENT",
  "CARE_TEAM_LEAD",
  "FRAUD_ANALYST",
  "NETWORK_OPS",
  "PRODUCT_MANAGER",
  "ADMIN",
] as const

export type StaffRole = (typeof STAFF_ROLES)[number]

/** The signed-in staff member, as returned by /api/auth/me and /api/auth/login. */
export interface SessionUser {
  id: number
  username: string
  display_name: string
  email: string
  role: StaffRole
  role_label: string
  permissions: Permission[]
}
