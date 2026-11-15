"use client"

import { useState, useEffect, useCallback } from "react"
import { apiFetch, errorMessage } from "@/lib/api"

export type CustomerStatus = "ACTIVE" | "SUSPENDED" | "CANCELLED"

export interface Customer {
  id: number
  first_name: string
  last_name: string
  email: string
  phone: string
  plan_id: number | null
  plan_name: string | null
  status: CustomerStatus
  balance: number
  activated_at: string | null
  created_at: string | null
}

export interface CustomerCreate {
  firstName: string
  lastName: string
  email: string
  phone: string
  planId: number
  balance?: number
}

export type CustomerUpdate = Partial<CustomerCreate> & { status?: CustomerStatus }

export function useCustomers() {
  const [customers, setCustomers] = useState<Customer[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const fetchCustomers = useCallback(async () => {
    try {
      setLoading(true)
      setCustomers(await apiFetch<Customer[]>("/api/customers"))
      setError(null)
    } catch (e) {
      setError(errorMessage(e))
    } finally {
      setLoading(false)
    }
  }, [])

  const createCustomer = useCallback(async (customer: CustomerCreate) => {
    const created = await apiFetch<Customer>("/api/customers", { method: "POST", body: customer })
    setCustomers((prev) => [...prev, created])
    return created
  }, [])

  const updateCustomer = useCallback(async (id: number, customer: CustomerUpdate) => {
    const updated = await apiFetch<Customer>(`/api/customers/${id}`, {
      method: "PATCH",
      body: customer,
    })
    setCustomers((prev) => prev.map((c) => (c.id === id ? updated : c)))
    return updated
  }, [])

  const deleteCustomer = useCallback(async (id: number) => {
    await apiFetch<void>(`/api/customers/${id}`, { method: "DELETE" })
    setCustomers((prev) => prev.filter((c) => c.id !== id))
  }, [])

  useEffect(() => {
    void fetchCustomers()
  }, [fetchCustomers])

  return {
    customers,
    loading,
    error,
    fetchCustomers,
    createCustomer,
    updateCustomer,
    deleteCustomer,
  }
}
