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
import { formatDate, formatMoney, formatMsisdn } from "@/lib/format"
import { label } from "@/lib/labels"
import type { Line } from "@/hooks/use-lines"

export function LinesTable({ lines }: { lines: Line[] }) {
  return (
    <Table>
      <TableHeader className="bg-background sticky top-0 z-10">
        <TableRow>
          <TableHead>Mobile number</TableHead>
          <TableHead>Customer</TableHead>
          <TableHead>Plan</TableHead>
          <TableHead className="text-right">Price/mo</TableHead>
          <TableHead>SIM</TableHead>
          <TableHead>Status</TableHead>
          <TableHead>Contract ends</TableHead>
        </TableRow>
      </TableHeader>
      <TableBody>
        {lines.map((line) => (
          <TableRow key={line.id}>
            <TableCell className="font-mono text-sm whitespace-nowrap">
              {formatMsisdn(line.msisdn)}
            </TableCell>
            <TableCell>
              <div className="font-medium">{line.customer_name}</div>
              <div className="text-muted-foreground text-xs">
                {line.account_number}
                {line.user_name ? ` · used by ${line.user_name}` : ""}
              </div>
            </TableCell>
            <TableCell>
              <div>{line.plan_name}</div>
              <div className="text-muted-foreground text-xs">
                {label(line.brand)} · {label(line.plan_category)}
              </div>
            </TableCell>
            <TableCell className="text-right">{formatMoney(line.monthly_price)}</TableCell>
            <TableCell>{line.sim_type === "ESIM" ? "eSIM" : "Physical"}</TableCell>
            <TableCell>
              <StatusBadge value={line.status} />
              {line.status_reason && (
                <div className="text-muted-foreground mt-1 max-w-[180px] truncate text-xs">
                  {line.status_reason}
                </div>
              )}
            </TableCell>
            <TableCell className="whitespace-nowrap">{formatDate(line.contract_end)}</TableCell>
          </TableRow>
        ))}
      </TableBody>
    </Table>
  )
}
