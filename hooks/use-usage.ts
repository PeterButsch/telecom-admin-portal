"use client"

import { useCallback } from "react"
import { usePagedList } from "@/hooks/use-paged-list"

export interface UsageRecord {
  id: number
  customer_id: number
  customer_name: string
  type: "CALL" | "DATA" | "SMS"
  quantity: number
  cost: number
  recorded_at: string | null
}

export interface UsageFilters {
  type?: "CALL" | "DATA" | "SMS" | null
  customerId?: number | null
  dateFrom?: string | null
  dateTo?: string | null
}

export function useUsage(filters: UsageFilters = {}) {
  const buildUrl = useCallback(
    (pageNum: number) => {
      const params = new URLSearchParams()
      params.set("page", pageNum.toString())
      params.set("size", "50")
      if (filters.type) params.set("type", filters.type)
      if (filters.customerId) params.set("customerId", filters.customerId.toString())
      if (filters.dateFrom) params.set("dateFrom", filters.dateFrom)
      if (filters.dateTo) params.set("dateTo", filters.dateTo)
      return `/api/usage?${params.toString()}`
    },
    [filters.type, filters.customerId, filters.dateFrom, filters.dateTo]
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
