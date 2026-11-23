"use client"

import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table"
import { Button } from "@/components/ui/button"
import { Pencil, Trash2 } from "lucide-react"
import { StatusBadge } from "@/components/status-badge"
import { formatMoney } from "@/lib/format"
import { label } from "@/lib/labels"
import type { Customer } from "@/hooks/use-customers"

interface CustomersTableProps {
  customers: Customer[]
  /** Leave out when the user may not edit; the edit button is then hidden. */
  onEdit?: (customer: Customer) => void
  /** Leave out when the user may not delete; the delete button is then hidden. */
  onDelete?: (customer: Customer) => void
}

function identity(customer: Customer) {
  if (customer.type === "BUSINESS") return customer.uen ? `UEN ${customer.uen}` : "-"
  if (!customer.id_number_masked) return "-"
  return `${label(customer.id_type)} ${customer.id_number_masked}`
}

export function CustomersTable({ customers, onEdit, onDelete }: CustomersTableProps) {
  const hasActions = Boolean(onEdit || onDelete)
  return (
    <Table>
      <TableHeader className="bg-background sticky top-0 z-10">
        <TableRow>
          <TableHead>Customer no.</TableHead>
          <TableHead>Name</TableHead>
          <TableHead>ID</TableHead>
          <TableHead>Contact</TableHead>
          <TableHead className="text-right">Lines</TableHead>
          <TableHead>Status</TableHead>
          <TableHead className="text-right">Balance</TableHead>
          {hasActions && <TableHead className="w-[100px] text-right">Actions</TableHead>}
        </TableRow>
      </TableHeader>
      <TableBody>
        {customers.map((customer) => (
          <TableRow key={customer.id}>
            <TableCell className="font-mono text-sm">{customer.customer_number}</TableCell>
            <TableCell>
              <div className="font-medium">{customer.display_name}</div>
              {customer.type === "BUSINESS" && customer.contact_name && (
                <div className="text-muted-foreground text-xs">{customer.contact_name}</div>
              )}
            </TableCell>
            <TableCell className="font-mono text-sm">{identity(customer)}</TableCell>
            <TableCell>
              <div>{customer.email}</div>
              <div className="text-muted-foreground text-xs">{customer.phone}</div>
            </TableCell>
            <TableCell className="text-right">
              {customer.active_line_count} / {customer.line_count}
            </TableCell>
            <TableCell>
              <StatusBadge value={customer.status} />
            </TableCell>
            <TableCell className="text-right">{formatMoney(customer.total_balance)}</TableCell>
            {hasActions && (
              <TableCell className="text-right">
                <div className="flex justify-end gap-1">
                  {onEdit && (
                    <Button
                      variant="ghost"
                      size="icon"
                      aria-label={`Edit ${customer.display_name}`}
                      onClick={() => onEdit(customer)}
                    >
                      <Pencil className="h-4 w-4" />
                    </Button>
                  )}
                  {onDelete && (
                    <Button
                      variant="ghost"
                      size="icon"
                      aria-label={`Delete ${customer.display_name}`}
                      onClick={() => onDelete(customer)}
                    >
                      <Trash2 className="h-4 w-4" />
                    </Button>
                  )}
                </div>
              </TableCell>
            )}
          </TableRow>
        ))}
      </TableBody>
    </Table>
  )
}
