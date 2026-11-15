"use client"

import { useRef, useState } from "react"
import { Plus, X } from "lucide-react"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { ConfirmDialog } from "@/components/confirm-dialog"
import { CustomerFormDialog } from "@/components/customer-form-dialog"
import { CustomersTable } from "@/components/customers-table"
import { FilterSelect } from "@/components/filter-select"
import { PageShell } from "@/components/page-shell"
import { ListCount, PagedListBody } from "@/components/paged-list-body"
import { SearchField } from "@/components/search-field"
import {
  CUSTOMER_STATUSES,
  CUSTOMER_TYPES,
  useCustomers,
  type Customer,
  type CustomerCreate,
  type CustomerStatus,
  type CustomerType,
  type CustomerUpdate,
} from "@/hooks/use-customers"
import { useDebouncedValue } from "@/hooks/use-debounced-value"
import { useInfiniteScroll } from "@/hooks/use-infinite-scroll"
import { options } from "@/lib/labels"

export default function CustomersPage() {
  const [search, setSearch] = useState("")
  const [type, setType] = useState<CustomerType | null>(null)
  const [status, setStatus] = useState<CustomerStatus | null>(null)
  const q = useDebouncedValue(search)
  const list = useCustomers({ q, type, status })
  const scrollRef = useRef<HTMLDivElement>(null)
  useInfiniteScroll({
    scrollRef,
    loadMore: list.loadMore,
    hasNext: list.hasNext,
    loading: list.loadingMore,
  })

  const [formOpen, setFormOpen] = useState(false)
  const [editingCustomer, setEditingCustomer] = useState<Customer | null>(null)
  const [customerToDelete, setCustomerToDelete] = useState<Customer | null>(null)
  const hasActiveFilters = search || type || status

  const handleFormSubmit = async (data: CustomerCreate | CustomerUpdate) => {
    if (editingCustomer) {
      await list.updateCustomer(editingCustomer.id, data as CustomerUpdate)
    } else {
      await list.createCustomer(data as CustomerCreate)
    }
  }

  return (
    <PageShell title="Customers">
      <Card className="flex flex-1 flex-col overflow-hidden">
        <CardHeader className="shrink-0 space-y-4">
          <div className="flex items-center justify-between">
            <CardTitle className="text-headline-2">Customers</CardTitle>
            <div className="flex items-center gap-4">
              <ListCount
                loading={list.loading}
                shown={list.customers.length}
                total={list.totalElements}
                noun="customers"
              />
              <Button
                onClick={() => {
                  setEditingCustomer(null)
                  setFormOpen(true)
                }}
              >
                <Plus className="mr-2 h-4 w-4" />
                Add Customer
              </Button>
            </div>
          </div>
          <div className="flex flex-wrap items-end gap-4">
            <SearchField
              id="customer-search"
              label="Search"
              value={search}
              onChange={setSearch}
              placeholder="Name, customer no., email, UEN or mobile number"
              className="w-[340px]"
            />
            <FilterSelect
              id="type-filter"
              label="Type"
              value={type}
              options={options(CUSTOMER_TYPES)}
              onChange={(v) => setType(v as CustomerType | null)}
            />
            <FilterSelect
              id="status-filter"
              label="Status"
              value={status}
              options={options(CUSTOMER_STATUSES)}
              onChange={(v) => setStatus(v as CustomerStatus | null)}
            />
            {hasActiveFilters && (
              <Button
                variant="ghost"
                size="sm"
                onClick={() => {
                  setSearch("")
                  setType(null)
                  setStatus(null)
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
            count={list.customers.length}
            onLoadMore={list.loadMore}
            scrollRef={scrollRef}
            emptyMessage="No customers match this search."
          >
            <CustomersTable
              customers={list.customers}
              onEdit={(customer) => {
                setEditingCustomer(customer)
                setFormOpen(true)
              }}
              onDelete={setCustomerToDelete}
            />
          </PagedListBody>
        </CardContent>
      </Card>

      <CustomerFormDialog
        open={formOpen}
        onOpenChange={setFormOpen}
        customer={editingCustomer}
        onSubmit={handleFormSubmit}
      />

      <ConfirmDialog
        open={customerToDelete !== null}
        onOpenChange={(open) => !open && setCustomerToDelete(null)}
        title="Delete Customer"
        description={
          <>
            Delete {customerToDelete?.display_name} ({customerToDelete?.customer_number})? Customers
            with accounts or support cases cannot be deleted; set their status to Closed instead.
          </>
        }
        onConfirm={async () => {
          if (customerToDelete) await list.deleteCustomer(customerToDelete.id)
        }}
      />
    </PageShell>
  )
}
