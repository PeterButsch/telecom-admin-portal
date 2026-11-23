import { act, renderHook, waitFor } from "@testing-library/react"
import { afterEach, describe, expect, it, vi } from "vitest"
import { useDashboardOverview } from "@/hooks/use-dashboard-overview"
import { overviewFixture } from "../fixtures/dashboard-overview"

function stubFetch(status = 200) {
  const body = status === 200 ? overviewFixture : { status, detail: "Overview query failed" }
  vi.stubGlobal(
    "fetch",
    vi.fn(
      async () =>
        new Response(JSON.stringify(body), {
          status,
          headers: {
            "Content-Type": status === 200 ? "application/json" : "application/problem+json",
          },
        })
    )
  )
}

describe("useDashboardOverview", () => {
  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it("loads the overview in one request", async () => {
    stubFetch()
    const { result } = renderHook(() => useDashboardOverview())

    await waitFor(() => expect(result.current.loading).toBe(false))

    expect(vi.mocked(fetch)).toHaveBeenCalledTimes(1)
    expect(vi.mocked(fetch).mock.calls[0]?.[0]).toBe("/api/dashboard/overview")
    expect(result.current.overview).toEqual(overviewFixture)
    expect(result.current.error).toBeNull()
  })

  it("reports a failure", async () => {
    stubFetch(500)
    const { result } = renderHook(() => useDashboardOverview())

    await waitFor(() => expect(result.current.loading).toBe(false))

    expect(result.current.overview).toBeNull()
    expect(result.current.error).toBe("Could not load the dashboard: Overview query failed")
  })

  it("refreshes on demand", async () => {
    stubFetch()
    const { result } = renderHook(() => useDashboardOverview())
    await waitFor(() => expect(result.current.loading).toBe(false))

    await act(async () => {
      await result.current.refresh()
    })

    expect(vi.mocked(fetch)).toHaveBeenCalledTimes(2)
  })
})
