"use client"

import { useState, useEffect, useCallback } from "react"
import { apiFetch, errorMessage } from "@/lib/api"

export interface Plan {
  id: number
  name: string
  monthly_price: number
  data_limit_gb: number | null
  minutes_limit: number | null
  sms_limit: number | null
  is_active: boolean
}

export interface PlanCreate {
  name: string
  monthlyPrice: number
  dataLimitGb?: number | null
  minutesLimit?: number | null
  smsLimit?: number | null
}

export interface PlanUpdate extends PlanCreate {
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

  const createPlan = useCallback(async (plan: PlanCreate) => {
    const created = await apiFetch<Plan>("/api/plans", { method: "POST", body: plan })
    setPlans((prev) => [...prev, created])
    return created
  }, [])

  const updatePlan = useCallback(async (id: number, plan: PlanUpdate) => {
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
