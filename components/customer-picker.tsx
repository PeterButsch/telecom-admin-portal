"use client"

import { useEffect, useState } from "react"
import { Loader2, X } from "lucide-react"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { useDebouncedValue } from "@/hooks/use-debounced-value"
import { customersUrl, type Customer } from "@/hooks/use-customers"
import type { PagedResponse } from "@/hooks/use-paged-list"
import { apiFetch, errorMessage } from "@/lib/api"

interface CustomerPickerProps {
  id: string
  value: Customer | null
  onChange: (customer: Customer | null) => void
  error?: string
}

/** Search box that finds a customer by name, customer number, email, UEN or mobile number. */
export function CustomerPicker({ id, value, onChange, error }: CustomerPickerProps) {
  const [query, setQuery] = useState("")
  const debounced = useDebouncedValue(query.trim(), 300)
  const [results, setResults] = useState<Customer[]>([])
  const [searching, setSearching] = useState(false)
  const [searchError, setSearchError] = useState<string | null>(null)
  const active = !value && debounced.length >= 2

  useEffect(() => {
    if (!active) return
    const controller = new AbortController()
    setSearching(true)
    apiFetch<PagedResponse<Customer>>(customersUrl({ q: debounced }, 0, 8), {
      signal: controller.signal,
    })
      .then((page) => {
        setResults(page.content)
        setSearchError(null)
      })
      .catch((e) => {
        if (!controller.signal.aborted) setSearchError(errorMessage(e))
      })
      .finally(() => {
        if (!controller.signal.aborted) setSearching(false)
      })
    return () => controller.abort()
  }, [active, debounced])

  if (value) {
    return (
      <div className="space-y-2">
        <Label>Customer</Label>
        <div className="flex items-center justify-between rounded-md border px-3 py-2 text-sm">
          <div>
            <div className="font-medium">{value.display_name}</div>
            <div className="text-muted-foreground text-xs">
              {value.customer_number} · {value.email}
            </div>
          </div>
          <Button
            type="button"
            variant="ghost"
            size="icon"
            aria-label="Choose a different customer"
            onClick={() => {
              onChange(null)
              setQuery("")
            }}
          >
            <X className="h-4 w-4" />
          </Button>
        </div>
      </div>
    )
  }

  return (
    <div className="space-y-2">
      <Label htmlFor={id}>Customer</Label>
      <Input
        id={id}
        value={query}
        onChange={(e) => setQuery(e.target.value)}
        placeholder="Name, customer no., email or mobile number"
        autoComplete="off"
        aria-invalid={error ? true : undefined}
      />
      {active && (
        <div className="max-h-48 overflow-auto rounded-md border" role="listbox">
          {searching ? (
            <div className="text-muted-foreground flex items-center gap-2 p-3 text-sm">
              <Loader2 className="h-4 w-4 animate-spin" />
              Searching...
            </div>
          ) : searchError ? (
            <div className="text-destructive p-3 text-sm">{searchError}</div>
          ) : results.length === 0 ? (
            <div className="text-muted-foreground p-3 text-sm">No matching customers.</div>
          ) : (
            results.map((customer) => (
              <button
                type="button"
                role="option"
                aria-selected={false}
                key={customer.id}
                className="hover:bg-accent block w-full px-3 py-2 text-left text-sm"
                onClick={() => onChange(customer)}
              >
                <span className="font-medium">{customer.display_name}</span>
                <span className="text-muted-foreground ml-2 text-xs">
                  {customer.customer_number} · {customer.email}
                </span>
              </button>
            ))
          )}
        </div>
      )}
      {error && <p className="text-destructive text-xs">{error}</p>}
    </div>
  )
}
