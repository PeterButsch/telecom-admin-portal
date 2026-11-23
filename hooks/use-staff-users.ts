"use client"

import { useCallback, useEffect, useState } from "react"
import { apiFetch, errorMessage } from "@/lib/api"
import type { StaffRole } from "@/lib/permissions"

export interface StaffUser {
  id: number
  username: string
  display_name: string
  email: string
  role: StaffRole
  role_label: string
  is_active: boolean
  is_locked: boolean
  locked_until: string | null
  failed_logins: number
  last_login_at: string | null
  created_at: string | null
}

export interface StaffUserCreate {
  username: string
  displayName: string
  email: string
  role: StaffRole
  password: string
}

export interface StaffUserUpdate {
  displayName?: string
  email?: string
  role?: StaffRole
  active?: boolean
}

/** Staff accounts, for administrators. */
export function useStaffUsers() {
  const [users, setUsers] = useState<StaffUser[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const refresh = useCallback(async () => {
    try {
      setUsers(await apiFetch<StaffUser[]>("/api/staff-users"))
      setError(null)
    } catch (e) {
      setError(errorMessage(e))
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    void refresh()
  }, [refresh])

  const replace = useCallback((updated: StaffUser) => {
    setUsers((prev) => prev.map((u) => (u.id === updated.id ? updated : u)))
    return updated
  }, [])

  const createUser = useCallback(async (user: StaffUserCreate) => {
    const created = await apiFetch<StaffUser>("/api/staff-users", { method: "POST", body: user })
    setUsers((prev) =>
      [...prev, created].sort((a, b) => a.display_name.localeCompare(b.display_name))
    )
    return created
  }, [])

  const updateUser = useCallback(
    async (id: number, changes: StaffUserUpdate) =>
      replace(
        await apiFetch<StaffUser>(`/api/staff-users/${id}`, { method: "PATCH", body: changes })
      ),
    [replace]
  )

  const unlockUser = useCallback(
    async (id: number) =>
      replace(await apiFetch<StaffUser>(`/api/staff-users/${id}/unlock`, { method: "POST" })),
    [replace]
  )

  const resetPassword = useCallback(
    async (id: number, password: string) =>
      replace(
        await apiFetch<StaffUser>(`/api/staff-users/${id}/password`, {
          method: "POST",
          body: { password },
        })
      ),
    [replace]
  )

  return { users, loading, error, refresh, createUser, updateUser, unlockUser, resetPassword }
}
