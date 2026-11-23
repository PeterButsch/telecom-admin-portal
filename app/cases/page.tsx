"use client"

import { useRef, useState } from "react"
import { Plus, X } from "lucide-react"
import { useAuth } from "@/components/auth-provider"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { CaseFormDialog } from "@/components/case-form-dialog"
import { CasesTable } from "@/components/cases-table"
import { FilterSelect } from "@/components/filter-select"
import { PageShell } from "@/components/page-shell"
import { ListCount, PagedListBody } from "@/components/paged-list-body"
import { SearchField } from "@/components/search-field"
import {
  CASE_CATEGORIES,
  CASE_PRIORITIES,
  CASE_STATUSES,
  useCases,
  type CaseCategory,
  type CaseCreate,
  type CasePriority,
  type CaseStatus,
  type CaseUpdate,
  type SupportCase,
} from "@/hooks/use-cases"
import { useDebouncedValue } from "@/hooks/use-debounced-value"
import { useInfiniteScroll } from "@/hooks/use-infinite-scroll"
import { options } from "@/lib/labels"

export default function CasesPage() {
  const { can } = useAuth()
  const canWrite = can("CASE_WRITE")
  const showCustomer = can("CUSTOMER_READ")
  const [search, setSearch] = useState("")
  const [priority, setPriority] = useState<CasePriority | null>(null)
  const [status, setStatus] = useState<CaseStatus | null>(null)
  const [category, setCategory] = useState<CaseCategory | null>(null)
  const q = useDebouncedValue(search)
  const list = useCases({ q, priority, status, category })
  const scrollRef = useRef<HTMLDivElement>(null)
  useInfiniteScroll({
    scrollRef,
    loadMore: list.loadMore,
    hasNext: list.hasNext,
    loading: list.loadingMore,
  })

  const [formOpen, setFormOpen] = useState(false)
  const [editingCase, setEditingCase] = useState<SupportCase | null>(null)
  const hasActiveFilters = search || priority || status || category

  const handleFormSubmit = async (data: CaseCreate | CaseUpdate) => {
    if (editingCase) {
      await list.updateCase(editingCase.id, data as CaseUpdate)
    } else {
      await list.createCase(data as CaseCreate)
    }
  }

  return (
    <PageShell title="Support Cases">
      <Card className="flex flex-1 flex-col overflow-hidden">
        <CardHeader className="shrink-0 space-y-4">
          <div className="flex items-center justify-between">
            <CardTitle className="text-headline-2">Support Cases</CardTitle>
            <div className="flex items-center gap-4">
              <ListCount
                loading={list.loading}
                shown={list.cases.length}
                total={list.totalElements}
                noun="cases"
              />
              {canWrite && (
                <Button
                  onClick={() => {
                    setEditingCase(null)
                    setFormOpen(true)
                  }}
                >
                  <Plus className="mr-2 h-4 w-4" />
                  New Case
                </Button>
              )}
            </div>
          </div>
          <div className="flex flex-wrap items-end gap-4">
            <SearchField
              id="case-search"
              label="Search"
              value={search}
              onChange={setSearch}
              placeholder="Case no. or subject"
            />
            <FilterSelect
              id="priority-filter"
              label="Priority"
              value={priority}
              options={options(CASE_PRIORITIES)}
              onChange={(v) => setPriority(v as CasePriority | null)}
              className="w-[120px]"
            />
            <FilterSelect
              id="status-filter"
              label="Status"
              value={status}
              options={options(CASE_STATUSES)}
              onChange={(v) => setStatus(v as CaseStatus | null)}
              className="w-[170px]"
            />
            <FilterSelect
              id="category-filter"
              label="Category"
              value={category}
              options={options(CASE_CATEGORIES)}
              onChange={(v) => setCategory(v as CaseCategory | null)}
              className="w-[160px]"
            />
            {hasActiveFilters && (
              <Button
                variant="ghost"
                size="sm"
                onClick={() => {
                  setSearch("")
                  setPriority(null)
                  setStatus(null)
                  setCategory(null)
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
            count={list.cases.length}
            onLoadMore={list.loadMore}
            scrollRef={scrollRef}
            emptyMessage="No cases match these filters."
          >
            <CasesTable
              cases={list.cases}
              showCustomer={showCustomer}
              onEdit={
                canWrite
                  ? (supportCase) => {
                      setEditingCase(supportCase)
                      setFormOpen(true)
                    }
                  : undefined
              }
            />
          </PagedListBody>
        </CardContent>
      </Card>

      <CaseFormDialog
        open={formOpen}
        onOpenChange={setFormOpen}
        supportCase={editingCase}
        onSubmit={handleFormSubmit}
      />
    </PageShell>
  )
}
