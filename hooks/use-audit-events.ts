"use client"

import { useCallback, useEffect, useState } from "react"
import { apiFetch } from "@/lib/api"
import { usePagedList } from "@/hooks/use-paged-list"

export const AUDIT_OUTCOMES = ["SUCCESS", "FAILED", "DENIED"] as const
export type AuditOutcome = (typeof AUDIT_OUTCOMES)[number]

export interface AuditEvent {
  id: number
  occurred_at: string
  actor_id: number | null
  actor_username: string | null
  actor_role: string | null
  actor_role_label: string | null
  action: string
  entity_type: string
  entity_id: string | null
  summary: string
  outcome: AuditOutcome
}

export interface AuditFilters {
  q?: string | null
  action?: string | null
  entityType?: string | null
  outcome?: AuditOutcome | null
  /** Inclusive calendar days, "yyyy-mm-dd". */
  dateFrom?: string | null
  dateTo?: string | null
}

export function useAuditEvents(filters: AuditFilters = {}) {
  const buildUrl = useCallback(
    (page: number) => {
      const params = new URLSearchParams({ page: page.toString(), size: "50" })
      if (filters.q?.trim()) params.set("q", filters.q.trim())
      if (filters.action) params.set("action", filters.action)
      if (filters.entityType) params.set("entityType", filters.entityType)
      if (filters.outcome) params.set("outcome", filters.outcome)
      if (filters.dateFrom) params.set("dateFrom", filters.dateFrom)
      if (filters.dateTo) params.set("dateTo", filters.dateTo)
      return `/api/audit-events?${params.toString()}`
    },
    [
      filters.q,
      filters.action,
      filters.entityType,
      filters.outcome,
      filters.dateFrom,
      filters.dateTo,
    ]
  )
  const list = usePagedList<AuditEvent>(buildUrl)

  return {
    events: list.items,
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

interface AuditFilterValues {
  actions: string[]
  entity_types: string[]
}

/** The actions and record types the audit filters accept. */
export function useAuditFilterValues() {
  const [values, setValues] = useState<AuditFilterValues>({ actions: [], entity_types: [] })

  useEffect(() => {
    apiFetch<AuditFilterValues>("/api/audit-events/filters")
      .then(setValues)
      .catch(() => setValues({ actions: [], entity_types: [] }))
  }, [])

  return values
}
