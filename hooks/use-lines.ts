"use client"

import { useCallback } from "react"
import { usePagedList } from "@/hooks/use-paged-list"
import type { Brand, PlanCategory } from "@/hooks/use-plans"

export const LINE_STATUSES = ["PENDING", "ACTIVE", "SUSPENDED", "BARRED", "TERMINATED"] as const
export type LineStatus = (typeof LINE_STATUSES)[number]

export interface Line {
  id: number
  msisdn: string
  sim_type: "PHYSICAL" | "ESIM"
  iccid: string | null
  status: LineStatus
  status_reason: string | null
  brand: Brand
  plan_id: number
  plan_name: string
  plan_category: PlanCategory
  monthly_price: number
  account_id: number
  account_number: string
  account_type: "POSTPAID" | "PREPAID"
  /** Left out for staff who may not read customers, as is user_name. */
  customer_id: number | null
  customer_name: string | null
  customer_type: "INDIVIDUAL" | "BUSINESS"
  user_name: string | null
  promotion_code: string | null
  promotion_ends_on: string | null
  contract_start: string | null
  contract_end: string | null
  port_in_from: string | null
  port_out_to: string | null
  activated_at: string | null
  terminated_at: string | null
}

export interface LineFilters {
  q?: string | null
  status?: LineStatus | null
  brand?: Brand | null
  customerId?: number | null
}

export function useLines(filters: LineFilters = {}) {
  const buildUrl = useCallback(
    (page: number) => {
      const params = new URLSearchParams({ page: page.toString(), size: "50" })
      if (filters.q?.trim()) params.set("q", filters.q.trim())
      if (filters.status) params.set("status", filters.status)
      if (filters.brand) params.set("brand", filters.brand)
      if (filters.customerId) params.set("customerId", filters.customerId.toString())
      return `/api/lines?${params.toString()}`
    },
    [filters.q, filters.status, filters.brand, filters.customerId]
  )
  const list = usePagedList<Line>(buildUrl)

  return {
    lines: list.items,
    loading: list.loading,
    loadingMore: list.loadingMore,
    error: list.error,
    loadMoreError: list.loadMoreError,
    hasNext: list.hasNext,
    totalElements: list.totalElements,
    loadMore: list.loadMore,
    refresh: list.refresh,
  }
}
