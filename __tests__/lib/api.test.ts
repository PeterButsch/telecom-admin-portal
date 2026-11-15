import { describe, it, expect, vi, afterEach, beforeEach } from "vitest"
import { apiFetch, ApiError, setUnauthorizedHandler } from "@/lib/api"

function json(body: unknown, status = 200) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json" },
  })
}

function setCsrfCookie(value: string) {
  document.cookie = `XSRF-TOKEN=${value}; path=/`
}

function clearCsrfCookie() {
  document.cookie = "XSRF-TOKEN=; expires=Thu, 01 Jan 1970 00:00:00 GMT; path=/"
}

describe("apiFetch", () => {
  beforeEach(() => {
    setCsrfCookie("token-1")
  })

  afterEach(() => {
    vi.unstubAllGlobals()
    clearCsrfCookie()
  })

  it("sends JSON bodies with the CSRF token and parses JSON responses", async () => {
    const fetchMock = vi.fn().mockResolvedValue(json({ id: 1 }))
    vi.stubGlobal("fetch", fetchMock)

    const result = await apiFetch<{ id: number }>("/api/things", {
      method: "POST",
      body: { name: "x" },
    })

    expect(result).toEqual({ id: 1 })
    expect(fetchMock).toHaveBeenCalledTimes(1)
    const [, init] = fetchMock.mock.calls[0]
    expect(init.body).toBe('{"name":"x"}')
    expect(init.headers["Content-Type"]).toBe("application/json")
    expect(init.headers["X-XSRF-TOKEN"]).toBe("token-1")
    expect(init.signal).toBeDefined()
  })

  it("does not send the CSRF token with reads", async () => {
    const fetchMock = vi.fn().mockResolvedValue(json([]))
    vi.stubGlobal("fetch", fetchMock)

    await apiFetch("/api/things")

    expect(fetchMock.mock.calls[0][1].headers).not.toHaveProperty("X-XSRF-TOKEN")
  })

  it("asks the server for a CSRF token when the cookie is missing", async () => {
    clearCsrfCookie()
    const fetchMock = vi.fn().mockImplementation(async (url: string) => {
      if (url === "/api/auth/csrf") {
        setCsrfCookie("fresh")
        return json({ token: "fresh" })
      }
      return json({ ok: true })
    })
    vi.stubGlobal("fetch", fetchMock)

    await apiFetch("/api/things", { method: "PATCH", body: {} })

    expect(fetchMock.mock.calls.map((call) => call[0])).toEqual(["/api/auth/csrf", "/api/things"])
    expect(fetchMock.mock.calls[1][1].headers["X-XSRF-TOKEN"]).toBe("fresh")
  })

  it("retries once with a fresh token when the server rejects an outdated one", async () => {
    let attempts = 0
    const fetchMock = vi.fn().mockImplementation(async (url: string) => {
      if (url === "/api/auth/csrf") {
        setCsrfCookie("token-2")
        return json({ token: "token-2" })
      }
      attempts++
      return attempts === 1
        ? json({ title: "Invalid CSRF token", detail: "Reload the page." }, 403)
        : json({ id: 7 })
    })
    vi.stubGlobal("fetch", fetchMock)

    await expect(apiFetch("/api/things", { method: "POST", body: {} })).resolves.toEqual({ id: 7 })

    const posts = fetchMock.mock.calls.filter((call) => call[0] === "/api/things")
    expect(posts.map((call) => call[1].headers["X-XSRF-TOKEN"])).toEqual(["token-1", "token-2"])
  })

  it("does not retry when the role is not allowed to do something", async () => {
    const fetchMock = vi
      .fn()
      .mockImplementation(async () =>
        json({ title: "Forbidden", detail: "Your role does not allow this action." }, 403)
      )
    vi.stubGlobal("fetch", fetchMock)

    await expect(apiFetch("/api/plans/1", { method: "DELETE" })).rejects.toThrow(
      "Your role does not allow this action."
    )
    expect(fetchMock).toHaveBeenCalledTimes(1)
  })

  it("reports an ended session, but not a failed sign-in, to the unauthorized handler", async () => {
    const handler = vi.fn()
    const remove = setUnauthorizedHandler(handler)
    vi.stubGlobal(
      "fetch",
      vi.fn().mockImplementation(async () => json({ detail: "Sign in to continue." }, 401))
    )

    await expect(apiFetch("/api/auth/me")).rejects.toThrow("Sign in to continue.")
    expect(handler).not.toHaveBeenCalled()

    await expect(apiFetch("/api/customers")).rejects.toMatchObject({ status: 401 })
    expect(handler).toHaveBeenCalledTimes(1)
    remove()
  })

  it("returns undefined for 204 No Content", async () => {
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue(new Response(null, { status: 204 })))

    await expect(apiFetch("/api/things/1", { method: "DELETE" })).resolves.toBeUndefined()
  })

  it("uses the problem detail from the server as the error message", async () => {
    vi.stubGlobal(
      "fetch",
      vi
        .fn()
        .mockResolvedValue(
          json({ title: "Conflict", detail: "A customer with this email already exists." }, 409)
        )
    )

    const error = (await apiFetch("/api/customers").catch((e: unknown) => e)) as ApiError

    expect(error).toBeInstanceOf(ApiError)
    expect(error.status).toBe(409)
    expect(error.title).toBe("Conflict")
    expect(error.message).toBe("A customer with this email already exists.")
  })

  it("lists field validation errors", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue(
        json(
          {
            title: "Validation failed",
            detail: "One or more fields are invalid.",
            errors: { email: "must be a well-formed email address" },
          },
          400
        )
      )
    )

    const error = (await apiFetch("/api/customers").catch((e: unknown) => e)) as ApiError

    expect(error.message).toBe("email must be a well-formed email address")
    expect(error.fieldErrors).toEqual({ email: "must be a well-formed email address" })
  })

  it("falls back to the status when the error body is not JSON", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue(new Response("<html>Bad gateway</html>", { status: 502 }))
    )

    await expect(apiFetch("/api/customers")).rejects.toThrow("Request failed with status 502")
  })

  it("explains when the server cannot be reached", async () => {
    vi.stubGlobal("fetch", vi.fn().mockRejectedValue(new TypeError("fetch failed")))

    await expect(apiFetch("/api/customers")).rejects.toThrow("Cannot reach the server")
  })

  it("explains when the request times out", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn().mockRejectedValue(new DOMException("The operation timed out.", "TimeoutError"))
    )

    await expect(apiFetch("/api/customers")).rejects.toThrow("took too long")
  })
})
