"use client"

import { useEffect } from "react"
import { usePathname, useRouter } from "next/navigation"
import { Loader2, ShieldAlert } from "lucide-react"
import { PageShell } from "@/components/page-shell"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { useAuth, type SignedOutReason } from "@/components/auth-provider"
import { isPublicPath, requiredPermission } from "@/lib/navigation"

function loginUrl(pathname: string, reason: SignedOutReason) {
  if (reason === "signed-out") return "/login"
  const params = new URLSearchParams()
  if (pathname !== "/") params.set("next", pathname)
  if (reason === "expired") params.set("expired", "1")
  const query = params.toString()
  return query ? `/login?${query}` : "/login"
}

function Centered({ children }: { children: React.ReactNode }) {
  return (
    <div className="text-muted-foreground flex min-h-svh flex-col items-center justify-center gap-3">
      {children}
    </div>
  )
}

/**
 * Shows a page only to signed-in staff whose role allows it. The page itself is not rendered
 * otherwise, so it never requests data the user may not see.
 */
export function AuthGate({ children }: { children: React.ReactNode }) {
  const pathname = usePathname()
  const router = useRouter()
  const { status, user, can, signedOutReason, error, retry } = useAuth()
  const isPublic = isPublicPath(pathname)

  useEffect(() => {
    if (!isPublic && status === "signed-out") {
      router.replace(loginUrl(pathname, signedOutReason))
    }
  }, [isPublic, status, pathname, signedOutReason, router])

  if (isPublic) return children

  if (status === "unavailable") {
    return (
      <Centered>
        <p role="alert" className="text-destructive">
          {error}
        </p>
        <Button variant="outline" onClick={retry}>
          Try again
        </Button>
      </Centered>
    )
  }

  if (status !== "signed-in" || !user) {
    return (
      <Centered>
        <Loader2 className="h-6 w-6 animate-spin" />
        <span>Loading...</span>
      </Centered>
    )
  }

  const needed = requiredPermission(pathname)
  if (needed && !can(needed)) {
    return (
      <PageShell title="No access" fitToWindow={false}>
        <Card className="max-w-xl">
          <CardHeader>
            <CardTitle className="text-headline-3 flex items-center gap-2">
              <ShieldAlert className="text-destructive h-5 w-5" />
              You do not have access to this page
            </CardTitle>
            <CardDescription>
              Your role, {user.role_label}, does not include this part of the portal. Ask an
              administrator if you need access.
            </CardDescription>
          </CardHeader>
          <CardContent>
            <Button variant="outline" onClick={() => router.push("/dashboard")}>
              Go to the dashboard
            </Button>
          </CardContent>
        </Card>
      </PageShell>
    )
  }

  return children
}
