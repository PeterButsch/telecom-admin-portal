import { renderHook, waitFor, act } from "@testing-library/react"
import { describe, it, expect, vi, afterEach } from "vitest"
import { usePagedList } from "@/hooks/use-paged-list"

function page(items: number[], pageNum: number, hasNext: boolean) {
  return new Response(
    JSON.stringify({
      content: items.map((id) => ({ id })),
      page: pageNum,
      totalPages: 3,
      totalElements: 6,
      hasNext,
    }),
    { status: 200, headers: { "Content-Type": "application/json" } }
  )
}

const buildUrl = (p: number) => `/api/items?page=${p}`

describe("usePagedList", () => {
  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it("does not skip a page when loading more fails", async () => {
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce(page([1, 2], 0, true))
      .mockResolvedValueOnce(new Response("{}", { status: 500 }))
      .mockResolvedValueOnce(page([3, 4], 1, true))
    vi.stubGlobal("fetch", fetchMock)

    const { result } = renderHook(() => usePagedList<{ id: number }>(buildUrl))
    await waitFor(() => expect(result.current.loading).toBe(false))

    await act(async () => {
      result.current.loadMore()
    })
    await waitFor(() => expect(result.current.loadMoreError).not.toBeNull())
    expect(result.current.items.map((i) => i.id)).toEqual([1, 2])

    await act(async () => {
      result.current.loadMore()
    })
    await waitFor(() => expect(result.current.items).toHaveLength(4))

    expect(fetchMock.mock.calls.map((c) => c[0])).toEqual([
      "/api/items?page=0",
      "/api/items?page=1",
      "/api/items?page=1",
    ])
    expect(result.current.loadMoreError).toBeNull()
  })

  it("ignores a second load-more while one is in flight", async () => {
    let release: (r: Response) => void = () => {}
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce(page([1, 2], 0, true))
      .mockImplementationOnce(() => new Promise<Response>((resolve) => (release = resolve)))
    vi.stubGlobal("fetch", fetchMock)

    const { result } = renderHook(() => usePagedList<{ id: number }>(buildUrl))
    await waitFor(() => expect(result.current.loading).toBe(false))

    act(() => {
      result.current.loadMore()
      result.current.loadMore()
    })
    await act(async () => {
      release(page([3, 4], 1, false))
    })
    await waitFor(() => expect(result.current.items).toHaveLength(4))

    expect(fetchMock).toHaveBeenCalledTimes(2)
  })

  it("does not skip the rows that move up after a delete", async () => {
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce(page([1, 2], 0, true))
      // After item 1 is deleted on the server, item 3 moves onto page 0.
      .mockResolvedValueOnce(page([2, 3], 0, true))
      .mockResolvedValueOnce(page([4, 5], 1, true))
    vi.stubGlobal("fetch", fetchMock)

    const { result } = renderHook(() => usePagedList<{ id: number }>(buildUrl))
    await waitFor(() => expect(result.current.loading).toBe(false))

    act(() => {
      result.current.removeItem(1)
    })
    expect(result.current.items.map((i) => i.id)).toEqual([2])
    expect(result.current.totalElements).toBe(5)

    await act(async () => {
      result.current.loadMore()
    })
    await waitFor(() => expect(result.current.items.map((i) => i.id)).toEqual([2, 3, 4, 5]))
    expect(fetchMock.mock.calls.map((c) => c[0])).toEqual([
      "/api/items?page=0",
      "/api/items?page=0",
      "/api/items?page=1",
    ])
  })

  it("discards a response that belongs to an outdated query", async () => {
    let releaseOld: (r: Response) => void = () => {}
    const fetchMock = vi
      .fn()
      .mockImplementationOnce(() => new Promise<Response>((resolve) => (releaseOld = resolve)))
      .mockResolvedValueOnce(page([9], 0, false))
    vi.stubGlobal("fetch", fetchMock)

    const { result, rerender } = renderHook(({ url }) => usePagedList<{ id: number }>(url), {
      initialProps: { url: buildUrl },
    })
    rerender({ url: (p: number) => `/api/items?status=OPEN&page=${p}` })
    await waitFor(() => expect(result.current.items.map((i) => i.id)).toEqual([9]))

    await act(async () => {
      releaseOld(page([1, 2], 0, true))
    })

    expect(result.current.items.map((i) => i.id)).toEqual([9])
  })
})
