"use client"

import { useState, useEffect, useCallback } from "react"
import { apiFetch, errorMessage } from "@/lib/api"

export const DEVICE_STATUSES = ["AVAILABLE", "ASSIGNED", "LOST", "DAMAGED"] as const
export type DeviceStatus = (typeof DEVICE_STATUSES)[number]

export interface Device {
  id: number
  imei: string
  manufacturer: string
  model: string
  status: DeviceStatus
  line_id: number | null
  msisdn: string | null
  customer_id: number | null
  customer_name: string | null
  assigned_at: string | null
  created_at: string | null
}

export interface DeviceCreate {
  imei: string
  manufacturer: string
  model: string
  /** Mobile number of the line to assign the device to; leave out to keep it in stock. */
  msisdn?: string
}

export interface DeviceUpdate {
  imei?: string
  manufacturer?: string
  model?: string
  msisdn?: string
  status?: DeviceStatus
}

export function useDevices() {
  const [devices, setDevices] = useState<Device[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const fetchDevices = useCallback(async () => {
    try {
      setLoading(true)
      setDevices(await apiFetch<Device[]>("/api/devices"))
      setError(null)
    } catch (e) {
      setError(errorMessage(e))
    } finally {
      setLoading(false)
    }
  }, [])

  const createDevice = useCallback(async (device: DeviceCreate) => {
    const created = await apiFetch<Device>("/api/devices", { method: "POST", body: device })
    setDevices((prev) => [...prev, created])
    return created
  }, [])

  const updateDevice = useCallback(async (id: number, device: DeviceUpdate) => {
    const updated = await apiFetch<Device>(`/api/devices/${id}`, { method: "PATCH", body: device })
    setDevices((prev) => prev.map((d) => (d.id === id ? updated : d)))
    return updated
  }, [])

  const deleteDevice = useCallback(async (id: number) => {
    await apiFetch<void>(`/api/devices/${id}`, { method: "DELETE" })
    setDevices((prev) => prev.filter((d) => d.id !== id))
  }, [])

  useEffect(() => {
    void fetchDevices()
  }, [fetchDevices])

  return {
    devices,
    loading,
    error,
    fetchDevices,
    createDevice,
    updateDevice,
    deleteDevice,
  }
}
