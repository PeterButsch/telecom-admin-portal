import { describe, it, expect } from "vitest"
import { requiredPermission, safeNextPath, visibleNavItems } from "@/lib/navigation"
import type { Permission } from "@/lib/permissions"

const productManager: Permission[] = ["CATALOG_READ", "PLAN_WRITE", "DEVICE_READ", "DASHBOARD_READ"]

describe("navigation", () => {
  it("knows which permission each page needs", () => {
    expect(requiredPermission("/customers")).toBe("CUSTOMER_READ")
    expect(requiredPermission("/customers/42")).toBe("CUSTOMER_READ")
    expect(requiredPermission("/staff")).toBe("STAFF_ADMIN")
    expect(requiredPermission("/audit")).toBe("AUDIT_READ")
    expect(requiredPermission("/insights")).toBe("DASHBOARD_READ")
    expect(requiredPermission("/login")).toBeNull()
    expect(requiredPermission("/customers-export")).toBeNull()
  })

  it("only lists pages the role may open and marks the current one", () => {
    const items = visibleNavItems((p) => productManager.includes(p), "/plans")

    expect(items.map((item) => item.title)).toEqual([
      "Dashboard",
      "Alerts & Insights",
      "Plans",
      "Devices",
    ])
    expect(items.filter((item) => item.isActive).map((item) => item.title)).toEqual(["Plans"])
  })

  it("only returns to pages on this site after signing in", () => {
    expect(safeNextPath("/customers")).toBe("/customers")
    expect(safeNextPath(null)).toBe("/dashboard")
    expect(safeNextPath("https://evil.example")).toBe("/dashboard")
    expect(safeNextPath("//evil.example")).toBe("/dashboard")
    expect(safeNextPath("/\\evil.example")).toBe("/dashboard")
    expect(safeNextPath("/login?next=/staff")).toBe("/dashboard")
  })
})
