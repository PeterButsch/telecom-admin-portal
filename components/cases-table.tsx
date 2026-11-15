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
import { Pencil } from "lucide-react"
import { StatusBadge } from "@/components/status-badge"
import { formatMsisdn, formatShortDateTime } from "@/lib/format"
import { label } from "@/lib/labels"
import type { SupportCase } from "@/hooks/use-cases"

interface CasesTableProps {
  cases: SupportCase[]
  onEdit: (supportCase: SupportCase) => void
}

const DONE = new Set(["RESOLVED", "CLOSED"])

export function CasesTable({ cases, onEdit }: CasesTableProps) {
  return (
    <Table>
      <TableHeader className="bg-background sticky top-0 z-10">
        <TableRow>
          <TableHead>Case no.</TableHead>
          <TableHead>Customer</TableHead>
          <TableHead>Subject</TableHead>
          <TableHead>Priority</TableHead>
          <TableHead>Status</TableHead>
          <TableHead>SLA due</TableHead>
          <TableHead>Opened</TableHead>
          <TableHead className="w-[60px] text-right">Actions</TableHead>
        </TableRow>
      </TableHeader>
      <TableBody>
        {cases.map((supportCase) => (
          <TableRow key={supportCase.id}>
            <TableCell className="font-mono text-sm">{supportCase.case_number}</TableCell>
            <TableCell>
              <div className="font-medium">{supportCase.customer_name}</div>
              <div className="text-muted-foreground text-xs">
                {supportCase.customer_number}
                {supportCase.msisdn ? ` · ${formatMsisdn(supportCase.msisdn)}` : ""}
              </div>
            </TableCell>
            <TableCell className="max-w-[220px]">
              <div className="truncate">{supportCase.subject}</div>
              <div className="text-muted-foreground text-xs">
                {label(supportCase.category)} · via {label(supportCase.channel)}
              </div>
            </TableCell>
            <TableCell>
              <StatusBadge value={supportCase.priority} />
            </TableCell>
            <TableCell>
              <StatusBadge value={supportCase.status} />
            </TableCell>
            <TableCell className="whitespace-nowrap">
              {DONE.has(supportCase.status) ? (
                <span className="text-muted-foreground">-</span>
              ) : supportCase.sla_breached ? (
                <StatusBadge value="SLA_BREACHED" tone="red">
                  Breached
                </StatusBadge>
              ) : (
                formatShortDateTime(supportCase.sla_due_at)
              )}
            </TableCell>
            <TableCell className="text-muted-foreground whitespace-nowrap">
              {formatShortDateTime(supportCase.created_at)}
            </TableCell>
            <TableCell className="text-right">
              <Button
                variant="ghost"
                size="icon"
                aria-label={`Edit case ${supportCase.case_number}`}
                disabled={supportCase.status === "CLOSED"}
                onClick={() => onEdit(supportCase)}
              >
                <Pencil className="h-4 w-4" />
              </Button>
            </TableCell>
          </TableRow>
        ))}
      </TableBody>
    </Table>
  )
}
