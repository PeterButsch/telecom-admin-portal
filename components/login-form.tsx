"use client"

import { useState } from "react"
import { LogIn } from "lucide-react"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { FormError } from "@/components/form-error"
import { TextField } from "@/components/form-fields"
import { Logo, Wordmark } from "@/components/logo"
import { errorMessage } from "@/lib/api"

export interface DemoAccount {
  username: string
  display_name: string
  role: string
  role_label: string
}

export interface DemoAccounts {
  password: string | null
  accounts: DemoAccount[]
}

interface LoginFormProps {
  onSignIn: (username: string, password: string) => Promise<unknown>
  demoAccounts?: DemoAccounts | null
  /** Information shown above the form, for example that the session has ended. */
  notice?: string | null
}

export function LoginForm({ onSignIn, demoAccounts, notice }: LoginFormProps) {
  const [username, setUsername] = useState("")
  const [password, setPassword] = useState("")
  const [signingInAs, setSigningInAs] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)

  const signIn = async (user: string, secret: string) => {
    setSigningInAs(user)
    setError(null)
    try {
      await onSignIn(user, secret)
    } catch (e) {
      setError(errorMessage(e))
      setSigningInAs(null)
    }
  }

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    void signIn(username.trim(), password)
  }

  const demoPassword = demoAccounts?.password
  const accounts = demoPassword ? (demoAccounts?.accounts ?? []) : []
  const busy = signingInAs !== null

  return (
    <div className="grid w-full max-w-4xl gap-6 md:grid-cols-2">
      <Card>
        <CardHeader className="space-y-3">
          <div className="flex items-center gap-3">
            <Logo className="size-9" />
            <Wordmark product="Ops Console" />
          </div>
          <CardTitle className="text-headline-2">Sign in</CardTitle>
          <CardDescription>Use your staff account to open the operations console.</CardDescription>
        </CardHeader>
        <CardContent>
          <form onSubmit={handleSubmit} className="space-y-4">
            {notice && (
              <p
                role="status"
                className="rounded-md border border-blue-300 bg-blue-50 px-3 py-2 text-sm text-blue-900 dark:border-blue-800 dark:bg-blue-950 dark:text-blue-100"
              >
                {notice}
              </p>
            )}
            <TextField
              id="username"
              label="Username"
              value={username}
              onChange={setUsername}
              autoComplete="username"
              autoCapitalize="none"
              spellCheck={false}
              autoFocus
              required
            />
            <TextField
              id="password"
              label="Password"
              type="password"
              value={password}
              onChange={setPassword}
              autoComplete="current-password"
              required
            />
            <FormError message={error} />
            <Button type="submit" className="w-full" disabled={busy}>
              {busy && signingInAs === username.trim() ? "Signing in..." : "Sign in"}
            </Button>
          </form>
        </CardContent>
      </Card>

      {accounts.length > 0 && (
        <Card>
          <CardHeader>
            <CardTitle className="text-headline-3">Demo accounts</CardTitle>
            <CardDescription>
              Each role sees only what its job needs. All demo accounts use the password{" "}
              <code className="bg-muted rounded px-1 py-0.5 font-mono text-xs">{demoPassword}</code>
              .
            </CardDescription>
          </CardHeader>
          <CardContent>
            <ul className="space-y-2">
              {accounts.map((account) => (
                <li key={account.username}>
                  <Button
                    type="button"
                    variant="outline"
                    className="h-auto w-full justify-between px-3 py-2 text-left"
                    disabled={busy}
                    aria-label={`Sign in as ${account.display_name}, ${account.role_label}`}
                    onClick={() => void signIn(account.username, demoPassword as string)}
                  >
                    <span className="grid">
                      <span className="font-medium">{account.display_name}</span>
                      <span className="text-muted-foreground text-xs">
                        {account.role_label} · <span className="font-mono">{account.username}</span>
                      </span>
                    </span>
                    <LogIn className="text-muted-foreground h-4 w-4" />
                  </Button>
                </li>
              ))}
            </ul>
          </CardContent>
        </Card>
      )}
    </div>
  )
}
