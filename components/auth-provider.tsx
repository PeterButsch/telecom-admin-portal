"use client"

import { createContext, useCallback, useContext, useEffect, useMemo, useState } from "react"
import { ApiError, apiFetch, errorMessage, setUnauthorizedHandler } from "@/lib/api"
import type { Permission, SessionUser } from "@/lib/permissions"

export type AuthStatus = "loading" | "signed-in" | "signed-out" | "unavailable"
/** Why there is no session: the server ended it, or the user signed out. */
export type SignedOutReason = "expired" | "signed-out" | null

interface AuthContextValue {
  status: AuthStatus
  user: SessionUser | null
  signedOutReason: SignedOutReason
  /** Why the session could not be checked when the status is "unavailable". */
  error: string | null
  can: (permission: Permission) => boolean
  signIn: (username: string, password: string) => Promise<SessionUser>
  signOut: () => Promise<void>
  retry: () => void
}

const AuthContext = createContext<AuthContextValue | null>(null)

/** Loads the signed-in staff member and notices when the server ends the session. */
export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [status, setStatus] = useState<AuthStatus>("loading")
  const [user, setUser] = useState<SessionUser | null>(null)
  const [signedOutReason, setSignedOutReason] = useState<SignedOutReason>(null)
  const [error, setError] = useState<string | null>(null)

  const load = useCallback(async () => {
    try {
      const me = await apiFetch<SessionUser>("/api/auth/me")
      setUser(me)
      setStatus("signed-in")
    } catch (e) {
      setUser(null)
      if (e instanceof ApiError && e.status === 401) {
        setStatus("signed-out")
      } else {
        setError(errorMessage(e))
        setStatus("unavailable")
      }
    }
  }, [])

  useEffect(() => {
    void load()
  }, [load])

  useEffect(
    () =>
      setUnauthorizedHandler(() => {
        setUser(null)
        setSignedOutReason("expired")
        setStatus("signed-out")
      }),
    []
  )

  const retry = useCallback(() => {
    setStatus("loading")
    setError(null)
    void load()
  }, [load])

  const signIn = useCallback(async (username: string, password: string) => {
    const signedIn = await apiFetch<SessionUser>("/api/auth/login", {
      method: "POST",
      body: { username, password },
    })
    setUser(signedIn)
    setSignedOutReason(null)
    setStatus("signed-in")
    return signedIn
  }, [])

  const signOut = useCallback(async () => {
    try {
      await apiFetch("/api/auth/logout", { method: "POST" })
    } catch {
      // Signing out is best effort; an unreachable server ends the session when it times out.
    }
    // The gate unmounts the open page, so nothing the previous user loaded stays on screen.
    setUser(null)
    setSignedOutReason("signed-out")
    setStatus("signed-out")
  }, [])

  const value = useMemo<AuthContextValue>(
    () => ({
      status,
      user,
      signedOutReason,
      error,
      can: (permission) => user?.permissions.includes(permission) ?? false,
      signIn,
      signOut,
      retry,
    }),
    [status, user, signedOutReason, error, signIn, signOut, retry]
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext)
  if (!context) throw new Error("useAuth must be used inside an AuthProvider")
  return context
}
