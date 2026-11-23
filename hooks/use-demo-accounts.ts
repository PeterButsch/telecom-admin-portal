"use client"

import { useEffect, useState } from "react"
import type { DemoAccounts } from "@/components/login-form"
import { apiFetch } from "@/lib/api"

/** The demo staff accounts the server offers on the sign-in page; null when there are none. */
export function useDemoAccounts() {
  const [demoAccounts, setDemoAccounts] = useState<DemoAccounts | null>(null)

  useEffect(() => {
    apiFetch<DemoAccounts>("/api/auth/demo-accounts")
      .then(setDemoAccounts)
      .catch(() => setDemoAccounts(null))
  }, [])

  return demoAccounts
}
