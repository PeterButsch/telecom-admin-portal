"use client"

import { useState, useEffect, useCallback } from "react"
import { apiFetch, errorMessage } from "@/lib/api"

export const BRANDS = ["SINGTEL", "GOMO", "HI_CARD"] as const
export const PLAN_CATEGORIES = ["POSTPAID", "SIM_ONLY", "PREPAID", "DATA_ONLY"] as const
export const SEGMENTS = ["CONSUMER", "ENTERPRISE"] as const
export const NETWORKS = ["5G", "4G"] as const

export type Brand = (typeof BRANDS)[number]
export type PlanCategory = (typeof PLAN_CATEGORIES)[number]
export type Segment = (typeof SEGMENTS)[number]

export interface Plan {
  id: number
  code: string
  name: string
  brand: Brand
  category: PlanCategory
  segment: Segment
  monthly_price: number
  data_limit_gb: number | null
  minutes_limit: number | null
  sms_limit: number | null
  contract_months: number
  network: string
  is_active: boolean
  /** Lines on this plan that are not terminated. */
  line_count: number
  created_at: string | null
}

/** Body for both create and update. The code cannot change after a plan is created. */
export interface PlanRequest {
  code: string
  name: string
  brand: Brand
  category: PlanCategory
  segment: Segment
  monthlyPrice: number
  dataLimitGb?: number | null
  minutesLimit?: number | null
  smsLimit?: number | null
  contractMonths?: number
  network?: string
  isActive?: boolean
}

export function usePlans() {
  const [plans, setPlans] = useState<Plan[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const fetchPlans = useCallback(async () => {
    try {
      setLoading(true)
      setPlans(await apiFetch<Plan[]>("/api/plans"))
      setError(null)
    } catch (e) {
      setError(errorMessage(e))
    } finally {
      setLoading(false)
    }
  }, [])

  const createPlan = useCallback(async (plan: PlanRequest) => {
    const created = await apiFetch<Plan>("/api/plans", { method: "POST", body: plan })
    setPlans((prev) => [...prev, created])
    return created
  }, [])

  const updatePlan = useCallback(async (id: number, plan: PlanRequest) => {
    const updated = await apiFetch<Plan>(`/api/plans/${id}`, { method: "PUT", body: plan })
    setPlans((prev) => prev.map((p) => (p.id === id ? updated : p)))
    return updated
  }, [])

  const deletePlan = useCallback(async (id: number) => {
    await apiFetch<void>(`/api/plans/${id}`, { method: "DELETE" })
    setPlans((prev) => prev.filter((p) => p.id !== id))
  }, [])

  useEffect(() => {
    void fetchPlans()
  }, [fetchPlans])

  return {
    plans,
    loading,
    error,
    fetchPlans,
    createPlan,
    updatePlan,
    deletePlan,
  }
}
