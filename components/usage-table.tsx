"use client"

import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table"
import { StatusBadge } from "@/components/status-badge"
import { formatDateTime, formatMoney, formatMsisdn, formatUsage } from "@/lib/format"
import type { UsageRecord } from "@/hooks/use-usage"

interface UsageTableProps {
  records: UsageRecord[]
  /** Set to false for staff who may not read customers. */
  showCustomer?: boolean
}

export function UsageTable({ records, showCustomer = true }: UsageTableProps) {
  return (
    <Table>
      <TableHeader className="bg-background sticky top-0 z-10">
        <TableRow>
          <TableHead>Recorded</TableHead>
          <TableHead>Mobile number</TableHead>
          {showCustomer && <TableHead>Customer</TableHead>}
          <TableHead>Type</TableHead>
          <TableHead className="text-right">Quantity</TableHead>
          <TableHead className="text-right">Charge</TableHead>
        </TableRow>
      </TableHeader>
      <TableBody>
        {records.map((record) => (
          <TableRow key={record.id}>
            <TableCell className="text-muted-foreground whitespace-nowrap">
              {formatDateTime(record.recorded_at)}
            </TableCell>
            <TableCell className="font-mono text-sm whitespace-nowrap">
              {formatMsisdn(record.msisdn)}
            </TableCell>
            {showCustomer && <TableCell>{record.customer_name}</TableCell>}
            <TableCell>
              <div className="flex items-center gap-1">
                <StatusBadge value={record.type} />
                {record.roaming && <StatusBadge value="ROAMING" tone="orange" />}
              </div>
            </TableCell>
            <TableCell className="text-right">
              {formatUsage(record.quantity, record.unit)}
            </TableCell>
            <TableCell className="text-right">{formatMoney(record.cost)}</TableCell>
          </TableRow>
        ))}
      </TableBody>
    </Table>
  )
}
