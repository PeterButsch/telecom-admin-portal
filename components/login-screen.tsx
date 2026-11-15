"use client"

import { useEffect } from "react"
import { useRouter, useSearchParams } from "next/navigation"
import { useAuth } from "@/components/auth-provider"
import { LoginForm } from "@/components/login-form"
import { useDemoAccounts } from "@/hooks/use-demo-accounts"
import { safeNextPath } from "@/lib/navigation"

/** Sign-in page body; once signed in it continues to the page the user originally asked for. */
export function LoginScreen() {
  const params = useSearchParams()
  const router = useRouter()
  const { status, signIn } = useAuth()
  const demoAccounts = useDemoAccounts()
  const next = safeNextPath(params.get("next"))
  const expired = params.get("expired") === "1"

  useEffect(() => {
    if (status === "signed-in") router.replace(next)
  }, [status, next, router])

  return (
    <main className="bg-muted/40 flex min-h-svh items-center justify-center p-4">
      <LoginForm
        onSignIn={signIn}
        demoAccounts={demoAccounts}
        notice={expired ? "Your session has ended. Sign in again to continue." : null}
      />
    </main>
  )
}
