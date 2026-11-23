"use client"

import { useCallback } from "react"
import { usePagedList } from "@/hooks/use-paged-list"

export const USAGE_TYPES = ["CALL", "DATA", "SMS"] as const
export type UsageType = (typeof USAGE_TYPES)[number]

export interface UsageRecord {
  id: number
  line_id: number
  msisdn: string
  /** Left out for staff who may not read customers. */
  customer_id: number | null
  customer_name: string | null
  type: UsageType
  quantity: number
  unit: "MIN" | "MB" | "SMS"
  cost: number
  roaming: boolean
  recorded_at: string | null
}

export interface UsageFilters {
  type?: UsageType | null
  msisdn?: string | null
  customerId?: number | null
  dateFrom?: string | null
  dateTo?: string | null
}

export function useUsage(filters: UsageFilters = {}) {
  const buildUrl = useCallback(
    (page: number) => {
      const params = new URLSearchParams({ page: page.toString(), size: "50" })
      if (filters.type) params.set("type", filters.type)
      if (filters.msisdn?.trim()) params.set("msisdn", filters.msisdn.replace(/\s/g, ""))
      if (filters.customerId) params.set("customerId", filters.customerId.toString())
      if (filters.dateFrom) params.set("dateFrom", filters.dateFrom)
      if (filters.dateTo) params.set("dateTo", filters.dateTo)
      return `/api/usage?${params.toString()}`
    },
    [filters.type, filters.msisdn, filters.customerId, filters.dateFrom, filters.dateTo]
  )

  const list = usePagedList<UsageRecord>(buildUrl)

  return {
    records: list.items,
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
