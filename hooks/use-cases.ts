"use client"

import { useCallback } from "react"
import { apiFetch } from "@/lib/api"
import { usePagedList } from "@/hooks/use-paged-list"

export const CASE_PRIORITIES = ["LOW", "MEDIUM", "HIGH", "URGENT"] as const
export const CASE_STATUSES = [
  "OPEN",
  "IN_PROGRESS",
  "PENDING_CUSTOMER",
  "RESOLVED",
  "CLOSED",
] as const
export const CASE_CATEGORIES = [
  "BILLING",
  "NETWORK",
  "DEVICE",
  "PLAN_CHANGE",
  "PORTING",
  "SIM_REPLACEMENT",
  "SCAM_REPORT",
  "ROAMING",
  "GENERAL",
] as const
export const CASE_CHANNELS = ["PHONE", "CHAT", "STORE", "EMAIL", "APP"] as const

export type CasePriority = (typeof CASE_PRIORITIES)[number]
export type CaseStatus = (typeof CASE_STATUSES)[number]
export type CaseCategory = (typeof CASE_CATEGORIES)[number]
export type CaseChannel = (typeof CASE_CHANNELS)[number]

export interface SupportCase {
  id: number
  case_number: string
  customer_id: number
  customer_number: string
  customer_name: string
  line_id: number | null
  msisdn: string | null
  category: CaseCategory
  channel: CaseChannel
  subject: string
  description: string | null
  priority: CasePriority
  status: CaseStatus
  sla_due_at: string | null
  sla_breached: boolean
  created_at: string | null
  resolved_at: string | null
}

export interface CaseFilters {
  q?: string | null
  priority?: CasePriority | null
  status?: CaseStatus | null
  category?: CaseCategory | null
  customerId?: number | null
}

export interface CaseCreate {
  customerId: number
  msisdn?: string
  category: CaseCategory
  channel: CaseChannel
  subject: string
  description?: string
  priority: CasePriority
}

export interface CaseUpdate {
  subject?: string
  description?: string
  category?: CaseCategory
  priority?: CasePriority
  status?: CaseStatus
}

export function useCases(filters: CaseFilters = {}) {
  const buildUrl = useCallback(
    (page: number) => {
      const params = new URLSearchParams({ page: page.toString(), size: "50" })
      if (filters.q?.trim()) params.set("q", filters.q.trim())
      if (filters.priority) params.set("priority", filters.priority)
      if (filters.status) params.set("status", filters.status)
      if (filters.category) params.set("category", filters.category)
      if (filters.customerId) params.set("customerId", filters.customerId.toString())
      return `/api/cases?${params.toString()}`
    },
    [filters.q, filters.priority, filters.status, filters.category, filters.customerId]
  )
  const list = usePagedList<SupportCase>(buildUrl)
  const { setItems, refresh } = list

  const createCase = useCallback(
    async (supportCase: CaseCreate) => {
      const created = await apiFetch<SupportCase>("/api/cases", {
        method: "POST",
        body: supportCase,
      })
      void refresh()
      return created
    },
    [refresh]
  )

  const updateCase = useCallback(
    async (id: number, supportCase: CaseUpdate) => {
      const updated = await apiFetch<SupportCase>(`/api/cases/${id}`, {
        method: "PATCH",
        body: supportCase,
      })
      setItems((prev) => prev.map((c) => (c.id === id ? updated : c)))
      return updated
    },
    [setItems]
  )

  return {
    cases: list.items,
    loading: list.loading,
    loadingMore: list.loadingMore,
    error: list.error,
    loadMoreError: list.loadMoreError,
    hasNext: list.hasNext,
    totalElements: list.totalElements,
    loadMore: list.loadMore,
    refresh,
    createCase,
    updateCase,
  }
}
