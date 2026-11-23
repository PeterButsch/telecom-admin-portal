"use client"

import { useCallback } from "react"
import { apiFetch } from "@/lib/api"
import { usePagedList } from "@/hooks/use-paged-list"

export const CUSTOMER_TYPES = ["INDIVIDUAL", "BUSINESS"] as const
export const CUSTOMER_STATUSES = ["ACTIVE", "SUSPENDED", "CLOSED"] as const
export const ID_TYPES = ["NRIC", "FIN", "PASSPORT"] as const

export type CustomerType = (typeof CUSTOMER_TYPES)[number]
export type CustomerStatus = (typeof CUSTOMER_STATUSES)[number]
export type IdType = (typeof ID_TYPES)[number]

export interface Customer {
  id: number
  customer_number: string
  type: CustomerType
  display_name: string
  first_name: string | null
  last_name: string | null
  company_name: string | null
  uen: string | null
  contact_name: string | null
  id_type: IdType | null
  /** Only the last characters of an ID number are ever sent by the API, for example "*****567A". */
  id_number_masked: string | null
  date_of_birth: string | null
  email: string
  phone: string
  address_line: string | null
  postal_code: string | null
  status: CustomerStatus
  account_count: number
  line_count: number
  active_line_count: number
  total_balance: number
  created_at: string | null
}

export interface CustomerCreate {
  type: CustomerType
  firstName?: string
  lastName?: string
  companyName?: string
  uen?: string
  contactName?: string
  idType?: IdType
  idNumber?: string
  dateOfBirth?: string
  email: string
  phone: string
  addressLine?: string
  postalCode?: string
}

/** Partial update. Identity fields (type, ID number, date of birth, UEN) cannot change. */
export interface CustomerUpdate {
  firstName?: string
  lastName?: string
  companyName?: string
  contactName?: string
  email?: string
  phone?: string
  addressLine?: string
  postalCode?: string
  status?: CustomerStatus
}

export interface CustomerFilters {
  q?: string | null
  type?: CustomerType | null
  status?: CustomerStatus | null
}

export function customersUrl(filters: CustomerFilters, page: number, size = 50) {
  const params = new URLSearchParams({ page: page.toString(), size: size.toString() })
  if (filters.q?.trim()) params.set("q", filters.q.trim())
  if (filters.type) params.set("type", filters.type)
  if (filters.status) params.set("status", filters.status)
  return `/api/customers?${params.toString()}`
}

export function useCustomers({ q, type, status }: CustomerFilters = {}) {
  const buildUrl = useCallback(
    (page: number) => customersUrl({ q, type, status }, page),
    [q, type, status]
  )
  const list = usePagedList<Customer>(buildUrl)
  const { setItems, removeItem, refresh } = list

  const createCustomer = useCallback(
    async (customer: CustomerCreate) => {
      const created = await apiFetch<Customer>("/api/customers", {
        method: "POST",
        body: customer,
      })
      void refresh()
      return created
    },
    [refresh]
  )

  const updateCustomer = useCallback(
    async (id: number, customer: CustomerUpdate) => {
      const updated = await apiFetch<Customer>(`/api/customers/${id}`, {
        method: "PATCH",
        body: customer,
      })
      setItems((prev) => prev.map((c) => (c.id === id ? updated : c)))
      return updated
    },
    [setItems]
  )

  const deleteCustomer = useCallback(
    async (id: number) => {
      await apiFetch<void>(`/api/customers/${id}`, { method: "DELETE" })
      removeItem(id)
    },
    [removeItem]
  )

  return {
    customers: list.items,
    loading: list.loading,
    loadingMore: list.loadingMore,
    error: list.error,
    loadMoreError: list.loadMoreError,
    hasNext: list.hasNext,
    totalElements: list.totalElements,
    loadMore: list.loadMore,
    refresh,
    createCustomer,
    updateCustomer,
    deleteCustomer,
  }
}
