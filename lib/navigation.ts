import {
  BarChart3,
  CreditCard,
  LayoutDashboard,
  LifeBuoy,
  ScrollText,
  Signal,
  Smartphone,
  TriangleAlert,
  UserCog,
  Users,
  type LucideIcon,
} from "lucide-react"
import type { Permission } from "@/lib/permissions"

export interface NavItem {
  title: string
  url: string
  icon: LucideIcon
  /** Pages under this URL are only shown to staff with this permission. */
  permission: Permission
  section: "operations" | "administration"
}

export const NAV_ITEMS: readonly NavItem[] = [
  {
    title: "Dashboard",
    url: "/dashboard",
    icon: LayoutDashboard,
    permission: "DASHBOARD_READ",
    section: "operations",
  },
  {
    title: "Alerts & Insights",
    url: "/insights",
    icon: TriangleAlert,
    permission: "DASHBOARD_READ",
    section: "operations",
  },
  {
    title: "Customers",
    url: "/customers",
    icon: Users,
    permission: "CUSTOMER_READ",
    section: "operations",
  },
  {
    title: "Mobile Lines",
    url: "/lines",
    icon: Signal,
    permission: "LINE_READ",
    section: "operations",
  },
  {
    title: "Plans",
    url: "/plans",
    icon: CreditCard,
    permission: "CATALOG_READ",
    section: "operations",
  },
  {
    title: "Devices",
    url: "/devices",
    icon: Smartphone,
    permission: "DEVICE_READ",
    section: "operations",
  },
  {
    title: "Usage",
    url: "/usage",
    icon: BarChart3,
    permission: "USAGE_READ",
    section: "operations",
  },
  {
    title: "Support Cases",
    url: "/cases",
    icon: LifeBuoy,
    permission: "CASE_READ",
    section: "operations",
  },
  {
    title: "Staff Accounts",
    url: "/staff",
    icon: UserCog,
    permission: "STAFF_ADMIN",
    section: "administration",
  },
  {
    title: "Audit Log",
    url: "/audit",
    icon: ScrollText,
    permission: "AUDIT_READ",
    section: "administration",
  },
]

/** Pages that do not need a signed-in user. */
export const PUBLIC_PATHS = ["/login"]

export function isPublicPath(pathname: string): boolean {
  return PUBLIC_PATHS.some((path) => pathname === path || pathname.startsWith(path + "/"))
}

function matches(item: NavItem, pathname: string) {
  return pathname === item.url || pathname.startsWith(item.url + "/")
}

/** The permission needed to open a page, or null when any signed-in user may open it. */
export function requiredPermission(pathname: string): Permission | null {
  return NAV_ITEMS.find((item) => matches(item, pathname))?.permission ?? null
}

/** Navigation entries the user may open, with the current page marked active. */
export function visibleNavItems(can: (permission: Permission) => boolean, pathname: string) {
  return NAV_ITEMS.filter((item) => can(item.permission)).map((item) => ({
    ...item,
    isActive: matches(item, pathname),
  }))
}

/**
 * Where to go after signing in. Only same-site paths are accepted, so a crafted link cannot
 * send the user to another site.
 */
export function safeNextPath(next: string | null | undefined): string {
  if (!next || !next.startsWith("/") || next.startsWith("//") || next.startsWith("/\\")) {
    return "/dashboard"
  }
  if (isPublicPath(next.split("?")[0])) return "/dashboard"
  return next
}
