"use client"

import { useCallback, useEffect, useState } from "react"
import { apiFetch, errorMessage } from "@/lib/api"

export const ALERT_AREAS = ["USAGE", "PLANS"] as const
export const ALERT_SEVERITIES = ["HIGH", "MEDIUM", "LOW"] as const

export type AlertArea = (typeof ALERT_AREAS)[number]
export type AlertSeverity = (typeof ALERT_SEVERITIES)[number]

/** A line an alert is about. Customer fields are null for staff who may not read customers. */
export interface AlertLine {
  line_id: number
  msisdn: string
  customer_id: number | null
  customer_name: string | null
  plan_name: string
  /** Set when the alert's value_kind is AMOUNT (SGD). */
  amount: number | null
  /** Set when the alert's value_kind is DATE: a "yyyy-mm-dd" date or an ISO timestamp. */
  date: string | null
  note: string | null
}

export interface InsightAlert {
  id: string
  area: AlertArea
  severity: AlertSeverity
  check: string
  title: string
  detail: string
  /** How many lines the alert is about. */
  affected: number
  value_label: string
  value_kind: "AMOUNT" | "DATE"
  /** Null when the role may see the count but not the lines. */
  lines: AlertLine[] | null
}

export interface Insights {
  as_of: string
  /** The latest usage record; usage checks count back from it. Null without usage access. */
  usage_as_of: string | null
  /** Most severe first. */
  alerts: InsightAlert[]
}

/** Alerts in the given area and of the given severity; null means any. */
export function filterAlerts(
  alerts: InsightAlert[],
  area: AlertArea | null,
  severity: AlertSeverity | null
): InsightAlert[] {
  return alerts.filter(
    (alert) => (!area || alert.area === area) && (!severity || alert.severity === severity)
  )
}

export function useInsights() {
  const [insights, setInsights] = useState<Insights | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const refresh = useCallback(async () => {
    try {
      setLoading(true)
      setInsights(await apiFetch<Insights>("/api/dashboard/insights"))
      setError(null)
    } catch (e) {
      setError(`Could not load alerts: ${errorMessage(e)}`)
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    void refresh()
  }, [refresh])

  return { insights, loading, error, refresh }
}
