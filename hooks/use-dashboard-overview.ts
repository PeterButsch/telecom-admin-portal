"use client"

import { useCallback, useEffect, useState } from "react"
import { apiFetch, errorMessage } from "@/lib/api"

export type Brand = "SINGTEL" | "GOMO" | "HI_CARD"
export type Segment = "CONSUMER" | "ENTERPRISE"
export type Operator = "M1" | "STARHUB" | "SIMBA" | "CIRCLES_LIFE" | "OTHER"
export type CasePriority = "URGENT" | "HIGH" | "MEDIUM" | "LOW"

/** Headline figures. Rates are percentages, or null when there was nothing to divide by. */
export interface OverviewKpis {
  active_customers: number
  active_lines: number
  monthly_revenue: number
  arpu: number | null
  churn_rate_30d: number | null
  terminations_30d: number
  activations_30d: number
  net_adds_30d: number
  port_ins_30d: number
  port_outs_30d: number
  open_cases: number
  breached_open_cases: number
  resolved_30d: number
  sla_met_rate_30d: number | null
}

export interface BrandFigures {
  brand: Brand
  active_lines: number
  monthly_revenue: number
  arpu: number | null
}

export interface SegmentFigures {
  segment: Segment
  active_customers: number
  active_lines: number
  monthly_revenue: number
  arpu: number | null
}

export interface MonthFigures {
  /** Calendar month in Singapore time, "yyyy-mm". */
  month: string
  /** The current month, counted up to now. */
  partial: boolean
  lines_at_start: number
  activations: number
  terminations: number
  port_ins: number
  port_outs: number
  churn_rate: number | null
}

export interface OperatorFigures {
  operator: Operator
  port_ins: number
  port_outs: number
}

export interface CasePriorityFigures {
  priority: CasePriority
  open: number
  breached: number
}

export interface DashboardOverview {
  as_of: string
  kpis: OverviewKpis
  brands: BrandFigures[]
  segments: SegmentFigures[]
  /** The last 12 months, oldest first. */
  months: MonthFigures[]
  /** Lines ported in from and out to each operator over the same 12 months. */
  porting: OperatorFigures[]
  open_cases: CasePriorityFigures[]
}

export function useDashboardOverview() {
  const [overview, setOverview] = useState<DashboardOverview | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const refresh = useCallback(async () => {
    try {
      setLoading(true)
      setOverview(await apiFetch<DashboardOverview>("/api/dashboard/overview"))
      setError(null)
    } catch (e) {
      setError(`Could not load the dashboard: ${errorMessage(e)}`)
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    void refresh()
  }, [refresh])

  return { overview, loading, error, refresh }
}
