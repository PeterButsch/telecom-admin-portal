import { renderHook, waitFor, act } from "@testing-library/react"
import { describe, it, expect, vi, beforeEach, afterEach } from "vitest"
import { usePlans, type Plan, type PlanRequest } from "@/hooks/use-plans"

const basic: Plan = {
  id: 1,
  code: "XO-5G-100",
  name: "XO Plus 100GB",
  brand: "SINGTEL",
  category: "POSTPAID",
  segment: "CONSUMER",
  monthly_price: 75,
  data_limit_gb: 100,
  minutes_limit: 1000,
  sms_limit: 1000,
  contract_months: 24,
  network: "5G",
  is_active: true,
  line_count: 10,
  created_at: null,
}

const request: PlanRequest = {
  code: "GOMO-20",
  name: "GOMO 20GB",
  brand: "GOMO",
  category: "SIM_ONLY",
  segment: "CONSUMER",
  monthlyPrice: 10,
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
    document.cookie = "XSRF-TOKEN=test-token; path=/"
    vi.stubGlobal(
      "fetch",
      vi.fn(async () => jsonResponse([basic]))
    )
  })

  afterEach(() => {
    vi.unstubAllGlobals()
    document.cookie = "XSRF-TOKEN=; expires=Thu, 01 Jan 1970 00:00:00 GMT; path=/"
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

    const created = { ...basic, id: 2, code: "GOMO-20", name: "GOMO 20GB" }
    vi.mocked(fetch).mockResolvedValueOnce(jsonResponse(created, 201))

    await act(async () => {
      await result.current.createPlan(request)
    })

    const [url, init] = vi.mocked(fetch).mock.calls[1]
    expect(url).toBe("/api/plans")
    expect(init?.method).toBe("POST")
    expect(JSON.parse(init?.body as string)).toEqual(request)
    expect(result.current.plans.map((p) => p.code)).toEqual(["XO-5G-100", "GOMO-20"])
  })

  it("replaces the updated plan in local state", async () => {
    const { result } = renderHook(() => usePlans())
    await waitFor(() => expect(result.current.loading).toBe(false))

    const updated = { ...basic, name: "XO Plus 120GB" }
    vi.mocked(fetch).mockResolvedValueOnce(jsonResponse(updated))

    await act(async () => {
      await result.current.updatePlan(1, { ...request, code: basic.code, name: updated.name })
    })

    expect(vi.mocked(fetch).mock.calls[1][1]?.method).toBe("PUT")
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

    vi.mocked(fetch).mockResolvedValueOnce(
      problemResponse(409, "Plan 1 is used by 2 line(s). Mark it inactive instead of deleting it.")
    )

    await expect(result.current.deletePlan(1)).rejects.toThrow("Plan 1 is used by 2 line(s).")
    expect(result.current.plans).toEqual([basic])
  })
})
