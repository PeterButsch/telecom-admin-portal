"use client"

import { Loader2 } from "lucide-react"
import { Button } from "@/components/ui/button"

interface PagedListBodyProps {
  loading: boolean
  error: string | null
  loadingMore: boolean
  loadMoreError: string | null
  hasNext: boolean
  count: number
  onLoadMore: () => void
  scrollRef: React.RefObject<HTMLDivElement | null>
  emptyMessage?: string
  children: React.ReactNode
}

/** Loading, error, empty and end-of-list states around an infinitely scrolling table. */
export function PagedListBody({
  loading,
  error,
  loadingMore,
  loadMoreError,
  hasNext,
  count,
  onLoadMore,
  scrollRef,
  emptyMessage = "Nothing matches these filters.",
  children,
}: PagedListBodyProps) {
  if (loading) {
    return (
      <div className="text-muted-foreground flex h-full items-center justify-center">
        <Loader2 className="mr-2 h-6 w-6 animate-spin" />
        Loading...
      </div>
    )
  }
  if (error) {
    return (
      <div role="alert" className="text-destructive py-8 text-center">
        {error}
      </div>
    )
  }
  return (
    <div ref={scrollRef} className="h-full overflow-auto">
      {children}
      {count === 0 && <div className="text-muted-foreground py-8 text-center">{emptyMessage}</div>}
      {loadingMore && (
        <div className="text-muted-foreground flex items-center justify-center py-4">
          <Loader2 className="mr-2 h-4 w-4 animate-spin" />
          Loading more...
        </div>
      )}
      {loadMoreError && (
        <div className="text-destructive flex items-center justify-center gap-2 py-4 text-sm">
          {loadMoreError}
          <Button variant="outline" size="sm" onClick={onLoadMore}>
            Retry
          </Button>
        </div>
      )}
      {!hasNext && count > 0 && (
        <div className="text-muted-foreground py-4 text-center text-sm">End of results</div>
      )}
    </div>
  )
}

/** "12 of 340 lines" style counter for list headers. */
export function ListCount({
  loading,
  shown,
  total,
  noun,
}: {
  loading: boolean
  shown: number
  total: number
  noun: string
}) {
  return (
    <span className="text-muted-foreground text-sm">
      {loading ? "Loading..." : `${shown.toLocaleString()} of ${total.toLocaleString()} ${noun}`}
    </span>
  )
}
