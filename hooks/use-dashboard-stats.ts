"use client"

import { useState, useEffect, useCallback } from "react"
import { apiFetch, errorMessage } from "@/lib/api"

export interface DashboardStats {
  active_customers: number
  monthly_revenue: number
  open_tickets: number
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
  const [customersByPlan, setCustomersByPlan] = useState<ChartData[]>([])
  const [devicesByStatus, setDevicesByStatus] = useState<ChartData[]>([])
  const [ticketsByStatus, setTicketsByStatus] = useState<ChartData[]>([])
  const [revenueByPlan, setRevenueByPlan] = useState<RevenueData[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const fetchStats = useCallback(async () => {
    try {
      setLoading(true)
      const [statsData, customersPlan, devicesStatus, ticketsStatus, revenue] = await Promise.all([
        apiFetch<DashboardStats>("/api/dashboard/stats"),
        apiFetch<ChartData[]>("/api/dashboard/customers-by-plan"),
        apiFetch<ChartData[]>("/api/dashboard/devices-by-status"),
        apiFetch<ChartData[]>("/api/dashboard/tickets-by-status"),
        apiFetch<RevenueData[]>("/api/dashboard/revenue-by-plan"),
      ])

      setStats(statsData)
      setCustomersByPlan(customersPlan)
      setDevicesByStatus(devicesStatus)
      setTicketsByStatus(ticketsStatus)
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
    customersByPlan,
    devicesByStatus,
    ticketsByStatus,
    revenueByPlan,
    loading,
    error,
    fetchStats,
  }
}
