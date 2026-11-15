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
 */
export function usePagedList<T>(buildUrl: (page: number) => string) {
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

  const fetchPage = useCallback(
    async (reset: boolean) => {
      if (!reset && inFlightRef.current) return
      const requestId = ++requestIdRef.current
      const page = reset ? 0 : nextPageRef.current
      inFlightRef.current = true
      if (reset) {
        setLoading(true)
      } else {
        setLoadingMore(true)
      }
      setLoadMoreError(null)
      try {
        const data = await apiFetch<PagedResponse<T>>(buildUrl(page))
        if (requestId !== requestIdRef.current) return
        nextPageRef.current = page + 1
        setItems((prev) => (reset ? (data.content ?? []) : [...prev, ...(data.content ?? [])]))
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

  useEffect(() => {
    void fetchPage(true)
  }, [fetchPage])

  return {
    items,
    setItems,
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
