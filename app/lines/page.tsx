"use client"

import { useRef, useState } from "react"
import { X } from "lucide-react"
import { useAuth } from "@/components/auth-provider"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { FilterSelect } from "@/components/filter-select"
import { LinesTable } from "@/components/lines-table"
import { PageShell } from "@/components/page-shell"
import { ListCount, PagedListBody } from "@/components/paged-list-body"
import { SearchField } from "@/components/search-field"
import { useDebouncedValue } from "@/hooks/use-debounced-value"
import { useInfiniteScroll } from "@/hooks/use-infinite-scroll"
import { LINE_STATUSES, useLines, type LineStatus } from "@/hooks/use-lines"
import { BRANDS, type Brand } from "@/hooks/use-plans"
import { options } from "@/lib/labels"

export default function LinesPage() {
  const showCustomer = useAuth().can("CUSTOMER_READ")
  const [search, setSearch] = useState("")
  const [status, setStatus] = useState<LineStatus | null>(null)
  const [brand, setBrand] = useState<Brand | null>(null)
  const q = useDebouncedValue(search)
  const list = useLines({ q, status, brand })
  const scrollRef = useRef<HTMLDivElement>(null)
  useInfiniteScroll({
    scrollRef,
    loadMore: list.loadMore,
    hasNext: list.hasNext,
    loading: list.loadingMore,
  })
  const hasActiveFilters = search || status || brand

  return (
    <PageShell title="Mobile Lines">
      <Card className="flex flex-1 flex-col overflow-hidden">
        <CardHeader className="shrink-0 space-y-4">
          <div className="flex items-center justify-between">
            <CardTitle className="text-headline-2">Mobile Lines</CardTitle>
            <ListCount
              loading={list.loading}
              shown={list.lines.length}
              total={list.totalElements}
              noun="lines"
            />
          </div>
          <div className="flex flex-wrap items-end gap-4">
            <SearchField
              id="line-search"
              label="Search"
              value={search}
              onChange={setSearch}
              placeholder={
                showCustomer
                  ? "Mobile number, customer, line user or ICCID"
                  : "Mobile number, account no. or ICCID"
              }
              className="w-[340px]"
            />
            <FilterSelect
              id="status-filter"
              label="Status"
              value={status}
              options={options(LINE_STATUSES)}
              onChange={(v) => setStatus(v as LineStatus | null)}
            />
            <FilterSelect
              id="brand-filter"
              label="Brand"
              value={brand}
              options={options(BRANDS)}
              onChange={(v) => setBrand(v as Brand | null)}
            />
            {hasActiveFilters && (
              <Button
                variant="ghost"
                size="sm"
                onClick={() => {
                  setSearch("")
                  setStatus(null)
                  setBrand(null)
                }}
              >
                <X className="mr-1 h-4 w-4" />
                Clear
              </Button>
            )}
          </div>
        </CardHeader>
        <CardContent className="flex-1 overflow-hidden">
          <PagedListBody
            loading={list.loading}
            error={list.error}
            loadingMore={list.loadingMore}
            loadMoreError={list.loadMoreError}
            hasNext={list.hasNext}
            count={list.lines.length}
            onLoadMore={list.loadMore}
            scrollRef={scrollRef}
            emptyMessage="No lines match these filters."
          >
            <LinesTable lines={list.lines} showCustomer={showCustomer} />
          </PagedListBody>
        </CardContent>
      </Card>
    </PageShell>
  )
}
