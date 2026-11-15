import { renderHook, waitFor, act } from "@testing-library/react"
import { describe, it, expect, vi, beforeEach, afterEach } from "vitest"
import { usePlans, type Plan } from "@/hooks/use-plans"

const basic: Plan = {
  id: 1,
  name: "Basic",
  monthly_price: 20,
  data_limit_gb: 10,
  minutes_limit: 500,
  sms_limit: 500,
  is_active: true,
}

function jsonResponse(body: unknown, status = 200) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json" },
  })
}

function problemResponse(status: number, detail: string) {
  return new Response(JSON.stringify({ title: "Error", status, detail }), {
    status,
    headers: { "Content-Type": "application/problem+json" },
  })
}

describe("usePlans", () => {
  beforeEach(() => {
    vi.stubGlobal(
      "fetch",
      vi.fn(async () => jsonResponse([basic]))
    )
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it("loads plans on mount and clears the loading flag", async () => {
    const { result } = renderHook(() => usePlans())

    expect(result.current.loading).toBe(true)
    await waitFor(() => expect(result.current.loading).toBe(false))

    expect(vi.mocked(fetch).mock.calls[0][0]).toBe("/api/plans")
    expect(result.current.plans).toEqual([basic])
    expect(result.current.error).toBeNull()
  })

  it("surfaces the server's error message when the request fails", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn(async () => problemResponse(503, "Database unavailable"))
    )

    const { result } = renderHook(() => usePlans())

    await waitFor(() => expect(result.current.loading).toBe(false))
    expect(result.current.error).toBe("Database unavailable")
    expect(result.current.plans).toEqual([])
  })

  it("appends the created plan to local state", async () => {
    const { result } = renderHook(() => usePlans())
    await waitFor(() => expect(result.current.loading).toBe(false))

    const created = { ...basic, id: 2, name: "Student" }
    vi.mocked(fetch).mockResolvedValueOnce(jsonResponse(created, 201))

    await act(async () => {
      await result.current.createPlan({ name: "Student", monthlyPrice: 15.5 })
    })

    const [url, init] = vi.mocked(fetch).mock.calls[1]
    expect(url).toBe("/api/plans")
    expect(init?.method).toBe("POST")
    expect(JSON.parse(init?.body as string)).toEqual({ name: "Student", monthlyPrice: 15.5 })
    expect(result.current.plans.map((p) => p.name)).toEqual(["Basic", "Student"])
  })

  it("replaces the updated plan in local state", async () => {
    const { result } = renderHook(() => usePlans())
    await waitFor(() => expect(result.current.loading).toBe(false))

    const updated = { ...basic, name: "Basic Plus" }
    vi.mocked(fetch).mockResolvedValueOnce(jsonResponse(updated))

    await act(async () => {
      await result.current.updatePlan(1, { name: "Basic Plus", monthlyPrice: 25 })
    })

    expect(result.current.plans).toEqual([updated])
  })

  it("removes the deleted plan from local state", async () => {
    const { result } = renderHook(() => usePlans())
    await waitFor(() => expect(result.current.loading).toBe(false))

    vi.mocked(fetch).mockResolvedValueOnce(new Response(null, { status: 204 }))

    await act(async () => {
      await result.current.deletePlan(1)
    })

    expect(result.current.plans).toEqual([])
  })

  it("throws the conflict reason and leaves state untouched when a delete fails", async () => {
    const { result } = renderHook(() => usePlans())
    await waitFor(() => expect(result.current.loading).toBe(false))

    vi.mocked(fetch).mockResolvedValueOnce(problemResponse(409, "Plan 1 is used by 2 customer(s)."))

    await expect(result.current.deletePlan(1)).rejects.toThrow("Plan 1 is used by 2 customer(s).")
    expect(result.current.plans).toEqual([basic])
  })
})
