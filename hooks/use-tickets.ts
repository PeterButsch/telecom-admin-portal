"use client"

import { useCallback } from "react"
import { apiFetch } from "@/lib/api"
import { createLogger } from "@/lib/logger"
import { usePagedList } from "@/hooks/use-paged-list"

const log = createLogger("useTickets")

export type TicketPriority = "LOW" | "MEDIUM" | "HIGH" | "URGENT"
export type TicketStatus = "OPEN" | "IN_PROGRESS" | "RESOLVED" | "CLOSED"

export interface Ticket {
  id: number
  customer_id: number
  customer_name: string
  subject: string
  description: string | null
  priority: TicketPriority
  status: TicketStatus
  created_at: string | null
  resolved_at: string | null
}

export interface TicketFilters {
  priority?: TicketPriority | null
  status?: TicketStatus | null
  customerId?: number | null
}

export interface TicketCreate {
  customerId: number
  subject: string
  description?: string
  priority: string
}

export interface TicketUpdate {
  subject?: string
  description?: string
  priority?: string
  status?: string
}

export function useTickets(filters: TicketFilters = {}) {
  const buildUrl = useCallback(
    (pageNum: number) => {
      const params = new URLSearchParams()
      params.set("page", pageNum.toString())
      params.set("size", "50")
      if (filters.priority) params.set("priority", filters.priority)
      if (filters.status) params.set("status", filters.status)
      if (filters.customerId) params.set("customerId", filters.customerId.toString())
      return `/api/tickets?${params.toString()}`
    },
    [filters.priority, filters.status, filters.customerId]
  )

  const list = usePagedList<Ticket>(buildUrl)
  const { setItems, refresh } = list

  const createTicket = useCallback(
    async (ticket: TicketCreate) => {
      log.info({ customerId: ticket.customerId }, "Creating ticket")
      const created = await apiFetch<Ticket>("/api/tickets", { method: "POST", body: ticket })
      log.info({ ticketId: created.id }, "Ticket created")
      void refresh()
      return created
    },
    [refresh]
  )

  const updateTicket = useCallback(
    async (id: number, ticket: TicketUpdate) => {
      log.info({ ticketId: id }, "Updating ticket")
      const updated = await apiFetch<Ticket>(`/api/tickets/${id}`, {
        method: "PATCH",
        body: ticket,
      })
      setItems((prev) => prev.map((t) => (t.id === id ? updated : t)))
      return updated
    },
    [setItems]
  )

  const deleteTicket = useCallback(
    async (id: number) => {
      log.info({ ticketId: id }, "Deleting ticket")
      await apiFetch<void>(`/api/tickets/${id}`, { method: "DELETE" })
      setItems((prev) => prev.filter((t) => t.id !== id))
    },
    [setItems]
  )

  return {
    tickets: list.items,
    loading: list.loading,
    loadingMore: list.loadingMore,
    error: list.error,
    loadMoreError: list.loadMoreError,
    hasNext: list.hasNext,
    totalElements: list.totalElements,
    loadMore: list.loadMore,
    refresh,
    createTicket,
    updateTicket,
    deleteTicket,
  }
}
