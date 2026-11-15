"use client"

import { useState, useEffect, useCallback } from "react"
import { apiFetch, errorMessage } from "@/lib/api"

export interface DashboardStats {
  active_customers: number
  active_lines: number
  monthly_revenue: number
  open_cases: number
  devices_in_use: number
}

export interface ChartData {
  name: string
  value: number
}

export interface RevenueData {
  name: string
  revenue: number
}

export function useDashboardStats() {
  const [stats, setStats] = useState<DashboardStats | null>(null)
  const [linesByPlan, setLinesByPlan] = useState<ChartData[]>([])
  const [devicesByStatus, setDevicesByStatus] = useState<ChartData[]>([])
  const [casesByStatus, setCasesByStatus] = useState<ChartData[]>([])
  const [revenueByPlan, setRevenueByPlan] = useState<RevenueData[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const fetchStats = useCallback(async () => {
    try {
      setLoading(true)
      const [statsData, linesPlan, devicesStatus, casesStatus, revenue] = await Promise.all([
        apiFetch<DashboardStats>("/api/dashboard/stats"),
        apiFetch<ChartData[]>("/api/dashboard/lines-by-plan"),
        apiFetch<ChartData[]>("/api/dashboard/devices-by-status"),
        apiFetch<ChartData[]>("/api/dashboard/cases-by-status"),
        apiFetch<RevenueData[]>("/api/dashboard/revenue-by-plan"),
      ])

      setStats(statsData)
      setLinesByPlan(linesPlan)
      setDevicesByStatus(devicesStatus)
      setCasesByStatus(casesStatus)
      setRevenueByPlan(revenue)
      setError(null)
    } catch (e) {
      setError(`Could not load the dashboard: ${errorMessage(e)}`)
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    void fetchStats()
  }, [fetchStats])

  return {
    stats,
    linesByPlan,
    devicesByStatus,
    casesByStatus,
    revenueByPlan,
    loading,
    error,
    fetchStats,
  }
}
