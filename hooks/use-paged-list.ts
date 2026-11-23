"use client"

import { useCallback, useEffect, useRef, useState } from "react"
import { apiFetch, errorMessage } from "@/lib/api"

export interface PagedResponse<T> {
  content: T[]
  page: number
  totalPages: number
  totalElements: number
  hasNext: boolean
}

/**
 * Loads a paged API resource for infinite scrolling.
 * A page counter only advances after a page has loaded, overlapping "load more" calls are ignored,
 * and responses for an outdated query (for example after a filter change) are discarded.
 * After rows are removed with removeItem, the server's later rows shift onto earlier pages, so the
 * next "load more" also reloads the pages they shifted onto; rows already shown are not repeated.
 */
export function usePagedList<T extends { id: number }>(buildUrl: (page: number) => string) {
  const [items, setItems] = useState<T[]>([])
  const [loading, setLoading] = useState(true)
  const [loadingMore, setLoadingMore] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [loadMoreError, setLoadMoreError] = useState<string | null>(null)
  const [hasNext, setHasNext] = useState(false)
  const [totalElements, setTotalElements] = useState(0)
  const nextPageRef = useRef(0)
  const requestIdRef = useRef(0)
  const inFlightRef = useRef(false)
  const pageSizeRef = useRef(0)
  const removedRef = useRef(0)

  const fetchPage = useCallback(
    async (reset: boolean) => {
      if (!reset && inFlightRef.current) return
      const requestId = ++requestIdRef.current
      const last = reset ? 0 : nextPageRef.current
      const removed = reset ? 0 : removedRef.current
      const pagesBack =
        removed > 0 && pageSizeRef.current > 0 ? Math.ceil(removed / pageSizeRef.current) : 0
      const first = Math.max(0, last - pagesBack)
      inFlightRef.current = true
      if (reset) {
        setLoading(true)
      } else {
        setLoadingMore(true)
      }
      setLoadMoreError(null)
      try {
        const pages: PagedResponse<T>[] = []
        for (let page = first; page <= last; page++) {
          pages.push(await apiFetch<PagedResponse<T>>(buildUrl(page)))
          if (requestId !== requestIdRef.current) return
        }
        const data = pages[pages.length - 1]
        const loaded = pages.flatMap((p) => p.content ?? [])
        nextPageRef.current = last + 1
        removedRef.current = reset ? 0 : removedRef.current - removed
        if (data.hasNext) pageSizeRef.current = data.content?.length ?? 0
        setItems((prev) => {
          if (reset) return loaded
          const shown = new Set(prev.map((item) => item.id))
          return [...prev, ...loaded.filter((item) => !shown.has(item.id))]
        })
        setHasNext(data.hasNext ?? false)
        setTotalElements(data.totalElements ?? 0)
        setError(null)
      } catch (e) {
        if (requestId !== requestIdRef.current) return
        if (reset) {
          setError(errorMessage(e))
        } else {
          setLoadMoreError(errorMessage(e))
        }
      } finally {
        if (requestId === requestIdRef.current) {
          inFlightRef.current = false
          setLoading(false)
          setLoadingMore(false)
        }
      }
    },
    [buildUrl]
  )

  const loadMore = useCallback(() => {
    if (hasNext) {
      void fetchPage(false)
    }
  }, [fetchPage, hasNext])

  const refresh = useCallback(() => fetchPage(true), [fetchPage])

  /** Drops a row that was deleted on the server. */
  const removeItem = useCallback((id: number) => {
    removedRef.current += 1
    setItems((prev) => prev.filter((item) => item.id !== id))
    setTotalElements((total) => Math.max(0, total - 1))
  }, [])

  useEffect(() => {
    void fetchPage(true)
  }, [fetchPage])

  return {
    items,
    setItems,
    removeItem,
    loading,
    loadingMore,
    error,
    loadMoreError,
    hasNext,
    totalElements,
    loadMore,
    refresh,
  }
}
