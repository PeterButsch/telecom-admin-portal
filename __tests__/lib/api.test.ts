import { describe, it, expect, vi, afterEach } from "vitest"
import { apiFetch, ApiError } from "@/lib/api"

function json(body: unknown, status = 200) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json" },
  })
}

describe("apiFetch", () => {
  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it("sends JSON bodies and parses JSON responses", async () => {
    const fetchMock = vi.fn().mockResolvedValue(json({ id: 1 }))
    vi.stubGlobal("fetch", fetchMock)

    const result = await apiFetch<{ id: number }>("/api/things", {
      method: "POST",
      body: { name: "x" },
    })

    expect(result).toEqual({ id: 1 })
    const [, init] = fetchMock.mock.calls[0]
    expect(init.body).toBe('{"name":"x"}')
    expect(init.headers["Content-Type"]).toBe("application/json")
    expect(init.signal).toBeDefined()
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
