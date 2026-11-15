import { act, render, screen, waitFor } from "@testing-library/react"
import { describe, it, expect, vi, beforeEach, afterEach } from "vitest"
import { AuthGate } from "@/components/auth-gate"
import { AuthProvider } from "@/components/auth-provider"
import { apiFetch } from "@/lib/api"
import type { Permission, SessionUser } from "@/lib/permissions"

const navigation = vi.hoisted(() => ({ pathname: "/customers", replace: vi.fn(), push: vi.fn() }))

vi.mock("next/navigation", () => ({
  usePathname: () => navigation.pathname,
  useRouter: () => ({ replace: navigation.replace, push: navigation.push }),
}))

function user(role: SessionUser["role"], permissions: Permission[]): SessionUser {
  return {
    id: 1,
    username: role.toLowerCase(),
    display_name: "Test User",
    email: "test@example.com",
    role,
    role_label: role === "CARE_AGENT" ? "Care Agent" : "Product Manager",
    permissions,
  }
}

function json(body: unknown, status = 200) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json" },
  })
}

function serve(me: SessionUser | null) {
  const fetchMock = vi.fn().mockImplementation(async (url: string) => {
    if (url === "/api/auth/me") {
      return me ? json(me) : json({ detail: "Sign in to continue." }, 401)
    }
    if (url === "/api/health") return json({ status: "UP", service: "test", version: "1" })
    return json({ detail: "Sign in to continue." }, 401)
  })
  vi.stubGlobal("fetch", fetchMock)
  return fetchMock
}

function renderPage() {
  render(
    <AuthProvider>
      <AuthGate>
        <p>Customer list</p>
      </AuthGate>
    </AuthProvider>
  )
}

describe("AuthGate", () => {
  beforeEach(() => {
    navigation.pathname = "/customers"
    navigation.replace.mockReset()
    vi.stubGlobal(
      "matchMedia",
      vi.fn().mockReturnValue({
        matches: false,
        addEventListener: vi.fn(),
        removeEventListener: vi.fn(),
      })
    )
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it("shows the page to a signed-in user whose role allows it", async () => {
    serve(user("CARE_AGENT", ["CUSTOMER_READ", "DASHBOARD_READ"]))
    renderPage()

    expect(await screen.findByText("Customer list")).toBeInTheDocument()
    expect(navigation.replace).not.toHaveBeenCalled()
  })

  it("sends signed-out visitors to sign in and remembers where they were going", async () => {
    serve(null)
    renderPage()

    await waitFor(() => expect(navigation.replace).toHaveBeenCalledWith("/login?next=%2Fcustomers"))
    expect(screen.queryByText("Customer list")).not.toBeInTheDocument()
  })

  it("does not render a page the role does not include", async () => {
    const fetchMock = serve(user("PRODUCT_MANAGER", ["CATALOG_READ", "DASHBOARD_READ"]))
    renderPage()

    expect(await screen.findByText("You do not have access to this page")).toBeInTheDocument()
    expect(screen.getByText(/Your role, Product Manager/)).toBeInTheDocument()
    expect(screen.queryByText("Customer list")).not.toBeInTheDocument()
    const urls = fetchMock.mock.calls.map((call) => call[0])
    expect(urls.every((url) => url === "/api/auth/me" || url === "/api/health")).toBeTruthy()
  })

  it("returns to sign-in with a notice when the server ends the session", async () => {
    serve(user("CARE_AGENT", ["CUSTOMER_READ"]))
    renderPage()
    await screen.findByText("Customer list")

    await act(async () => {
      await apiFetch("/api/customers").catch(() => undefined)
    })

    await waitFor(() =>
      expect(navigation.replace).toHaveBeenCalledWith("/login?next=%2Fcustomers&expired=1")
    )
  })

  it("shows public pages without a session", async () => {
    navigation.pathname = "/login"
    serve(null)
    render(
      <AuthProvider>
        <AuthGate>
          <p>Sign-in form</p>
        </AuthGate>
      </AuthProvider>
    )

    expect(screen.getByText("Sign-in form")).toBeInTheDocument()
    await waitFor(() => expect(fetch).toHaveBeenCalledWith("/api/auth/me", expect.anything()))
    expect(navigation.replace).not.toHaveBeenCalled()
  })
})
