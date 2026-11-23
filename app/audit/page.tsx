"use client"

import { useRef, useState } from "react"
import { X } from "lucide-react"
import { AuditTable } from "@/components/audit-table"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { FilterSelect } from "@/components/filter-select"
import { PageShell } from "@/components/page-shell"
import { ListCount, PagedListBody } from "@/components/paged-list-body"
import { SearchField } from "@/components/search-field"
import {
  AUDIT_OUTCOMES,
  useAuditEvents,
  useAuditFilterValues,
  type AuditOutcome,
} from "@/hooks/use-audit-events"
import { useDebouncedValue } from "@/hooks/use-debounced-value"
import { useInfiniteScroll } from "@/hooks/use-infinite-scroll"
import { options } from "@/lib/labels"

function DateFilter({
  id,
  label,
  value,
  onChange,
}: {
  id: string
  label: string
  value: string
  onChange: (value: string) => void
}) {
  return (
    <div className="space-y-1">
      <Label htmlFor={id}>{label}</Label>
      <Input
        id={id}
        type="date"
        value={value}
        onChange={(e) => onChange(e.target.value)}
        className="w-[150px]"
      />
    </div>
  )
}

export default function AuditPage() {
  const [search, setSearch] = useState("")
  const [action, setAction] = useState<string | null>(null)
  const [entityType, setEntityType] = useState<string | null>(null)
  const [outcome, setOutcome] = useState<AuditOutcome | null>(null)
  const [dateFrom, setDateFrom] = useState("")
  const [dateTo, setDateTo] = useState("")
  const q = useDebouncedValue(search)
  const filterValues = useAuditFilterValues()
  const list = useAuditEvents({ q, action, entityType, outcome, dateFrom, dateTo })
  const scrollRef = useRef<HTMLDivElement>(null)
  useInfiniteScroll({
    scrollRef,
    loadMore: list.loadMore,
    hasNext: list.hasNext,
    loading: list.loadingMore,
  })
  const hasActiveFilters = search || action || entityType || outcome || dateFrom || dateTo

  return (
    <PageShell title="Audit Log">
      <Card className="flex flex-1 flex-col overflow-hidden">
        <CardHeader className="shrink-0 space-y-4">
          <div className="flex items-center justify-between">
            <div>
              <CardTitle className="text-headline-2">Audit Log</CardTitle>
              <p className="text-muted-foreground mt-1 text-sm">
                Who signed in, viewed or changed what. Entries cannot be edited or deleted.
              </p>
            </div>
            <ListCount
              loading={list.loading}
              shown={list.events.length}
              total={list.totalElements}
              noun="events"
            />
          </div>
          <div className="flex flex-wrap items-end gap-4">
            <SearchField
              id="audit-search"
              label="Search"
              value={search}
              onChange={setSearch}
              placeholder="Details or username"
              className="w-[200px]"
            />
            <FilterSelect
              id="action-filter"
              label="Action"
              value={action}
              options={options(filterValues.actions)}
              onChange={setAction}
              className="w-[170px]"
            />
            <FilterSelect
              id="entity-filter"
              label="Record"
              value={entityType}
              options={options(filterValues.entity_types)}
              onChange={setEntityType}
              className="w-[140px]"
            />
            <FilterSelect
              id="outcome-filter"
              label="Outcome"
              value={outcome}
              options={options(AUDIT_OUTCOMES)}
              onChange={(v) => setOutcome(v as AuditOutcome | null)}
              className="w-[120px]"
            />
            <DateFilter id="date-from" label="From" value={dateFrom} onChange={setDateFrom} />
            <DateFilter id="date-to" label="To" value={dateTo} onChange={setDateTo} />
            {hasActiveFilters && (
              <Button
                variant="ghost"
                size="sm"
                onClick={() => {
                  setSearch("")
                  setAction(null)
                  setEntityType(null)
                  setOutcome(null)
                  setDateFrom("")
                  setDateTo("")
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
            count={list.events.length}
            onLoadMore={list.loadMore}
            scrollRef={scrollRef}
            emptyMessage="No audit events match these filters."
          >
            <AuditTable events={list.events} />
          </PagedListBody>
        </CardContent>
      </Card>
    </PageShell>
  )
}
