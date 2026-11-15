import type { DashboardOverview } from "@/hooks/use-dashboard-overview"

/** A small, internally consistent overview for component and hook tests. */
export const overviewFixture: DashboardOverview = {
  as_of: "2026-02-20T00:00:00Z",
  kpis: {
    active_customers: 2,
    active_lines: 5,
    monthly_revenue: 156,
    arpu: 31.2,
    churn_rate_30d: 100,
    terminations_30d: 2,
    activations_30d: 5,
    net_adds_30d: 3,
    port_ins_30d: 1,
    port_outs_30d: 2,
    open_cases: 3,
    breached_open_cases: 1,
    resolved_30d: 2,
    sla_met_rate_30d: 50,
  },
  brands: [
    { brand: "SINGTEL", active_lines: 3, monthly_revenue: 120, arpu: 40 },
    { brand: "GOMO", active_lines: 0, monthly_revenue: 0, arpu: null },
    { brand: "HI_CARD", active_lines: 2, monthly_revenue: 36, arpu: 18 },
  ],
  segments: [
    {
      segment: "CONSUMER",
      active_customers: 1,
      active_lines: 4,
      monthly_revenue: 116,
      arpu: 29,
    },
    {
      segment: "ENTERPRISE",
      active_customers: 1,
      active_lines: 1,
      monthly_revenue: 40,
      arpu: 40,
    },
  ],
  months: [
    {
      month: "2026-01",
      partial: false,
      lines_at_start: 0,
      activations: 3,
      terminations: 0,
      port_ins: 1,
      port_outs: 0,
      churn_rate: null,
    },
    {
      month: "2026-02",
      partial: true,
      lines_at_start: 3,
      activations: 4,
      terminations: 2,
      port_ins: 1,
      port_outs: 2,
      churn_rate: 66.67,
    },
  ],
  porting: [
    { operator: "M1", port_ins: 1, port_outs: 1 },
    { operator: "STARHUB", port_ins: 0, port_outs: 1 },
    { operator: "SIMBA", port_ins: 0, port_outs: 0 },
    { operator: "CIRCLES_LIFE", port_ins: 1, port_outs: 0 },
    { operator: "OTHER", port_ins: 0, port_outs: 0 },
  ],
  open_cases: [
    { priority: "URGENT", open: 2, breached: 1 },
    { priority: "HIGH", open: 0, breached: 0 },
    { priority: "MEDIUM", open: 0, breached: 0 },
    { priority: "LOW", open: 1, breached: 0 },
  ],
}
