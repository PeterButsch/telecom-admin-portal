"use client"

import { useRef, useState } from "react"
import { X } from "lucide-react"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { FilterSelect } from "@/components/filter-select"
import { PageShell } from "@/components/page-shell"
import { ListCount, PagedListBody } from "@/components/paged-list-body"
import { SearchField } from "@/components/search-field"
import { UsageTable } from "@/components/usage-table"
import { useDebouncedValue } from "@/hooks/use-debounced-value"
import { useInfiniteScroll } from "@/hooks/use-infinite-scroll"
import { USAGE_TYPES, useUsage, type UsageType } from "@/hooks/use-usage"
import { options } from "@/lib/labels"

const MSISDN = /^(\+?65)?[89]\d{7}$/

export default function UsagePage() {
  const [msisdnInput, setMsisdnInput] = useState("")
  const [type, setType] = useState<UsageType | null>(null)
  const [dateFrom, setDateFrom] = useState("")
  const [dateTo, setDateTo] = useState("")
  const typedMsisdn = useDebouncedValue(msisdnInput.replace(/[\s-]/g, ""))
  // Only filter once a full number has been typed; partial numbers would match nothing.
  const msisdn = MSISDN.test(typedMsisdn) ? typedMsisdn : null
  const msisdnIncomplete = typedMsisdn.length > 0 && !msisdn
  const list = useUsage({
    type,
    msisdn,
    dateFrom: dateFrom || null,
    dateTo: dateTo || null,
  })
  const scrollRef = useRef<HTMLDivElement>(null)
  useInfiniteScroll({
    scrollRef,
    loadMore: list.loadMore,
    hasNext: list.hasNext,
    loading: list.loadingMore,
  })
  const hasActiveFilters = msisdnInput || type || dateFrom || dateTo

  return (
    <PageShell title="Usage Records">
      <Card className="flex flex-1 flex-col overflow-hidden">
        <CardHeader className="shrink-0 space-y-4">
          <div className="flex items-center justify-between">
            <CardTitle className="text-headline-2">Usage Records</CardTitle>
            <ListCount
              loading={list.loading}
              shown={list.records.length}
              total={list.totalElements}
              noun="records"
            />
          </div>
          <div className="flex flex-wrap items-end gap-4">
            <div>
              <SearchField
                id="msisdn-filter"
                label="Mobile number"
                value={msisdnInput}
                onChange={setMsisdnInput}
                placeholder="9123 4567"
                className="w-[180px]"
              />
            </div>
            <FilterSelect
              id="type-filter"
              label="Type"
              value={type}
              options={options(USAGE_TYPES)}
              onChange={(v) => setType(v as UsageType | null)}
              className="w-[120px]"
            />
            <div className="space-y-1">
              <Label htmlFor="date-from">From</Label>
              <Input
                id="date-from"
                type="date"
                className="w-[160px]"
                value={dateFrom}
                max={dateTo || undefined}
                onChange={(e) => setDateFrom(e.target.value)}
              />
            </div>
            <div className="space-y-1">
              <Label htmlFor="date-to">To</Label>
              <Input
                id="date-to"
                type="date"
                className="w-[160px]"
                value={dateTo}
                min={dateFrom || undefined}
                onChange={(e) => setDateTo(e.target.value)}
              />
            </div>
            {hasActiveFilters && (
              <Button
                variant="ghost"
                size="sm"
                onClick={() => {
                  setMsisdnInput("")
                  setType(null)
                  setDateFrom("")
                  setDateTo("")
                }}
              >
                <X className="mr-1 h-4 w-4" />
                Clear
              </Button>
            )}
          </div>
          {msisdnIncomplete && (
            <p className="text-muted-foreground text-xs">
              Enter the full 8-digit mobile number to filter by line.
            </p>
          )}
        </CardHeader>
        <CardContent className="flex-1 overflow-hidden">
          <PagedListBody
            loading={list.loading}
            error={list.error}
            loadingMore={list.loadingMore}
            loadMoreError={list.loadMoreError}
            hasNext={list.hasNext}
            count={list.records.length}
            onLoadMore={list.loadMore}
            scrollRef={scrollRef}
            emptyMessage="No usage matches these filters."
          >
            <UsageTable records={list.records} />
          </PagedListBody>
        </CardContent>
      </Card>
    </PageShell>
  )
}
