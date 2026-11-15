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
import type { Plan } from "@/hooks/use-plans"

interface PlansTableProps {
  plans: Plan[]
  onEdit: (plan: Plan) => void
  onDelete: (plan: Plan) => void
}

function formatLimit(limit: number | null, unit = "") {
  return limit === null ? "Unlimited" : `${limit.toLocaleString()}${unit}`
}

export function PlansTable({ plans, onEdit, onDelete }: PlansTableProps) {
  return (
    <Table>
      <TableHeader className="bg-background sticky top-0 z-10">
        <TableRow>
          <TableHead>Plan</TableHead>
          <TableHead>Brand</TableHead>
          <TableHead>Category</TableHead>
          <TableHead className="text-right">Price/mo</TableHead>
          <TableHead className="text-right">Data</TableHead>
          <TableHead className="text-right">Talktime / SMS</TableHead>
          <TableHead className="text-right">Contract</TableHead>
          <TableHead className="text-right">Lines</TableHead>
          <TableHead>Status</TableHead>
          <TableHead className="w-[100px] text-right">Actions</TableHead>
        </TableRow>
      </TableHeader>
      <TableBody>
        {plans.map((plan) => (
          <TableRow key={plan.id}>
            <TableCell>
              <div className="font-medium">
                {plan.name}
                <span className="text-muted-foreground ml-1 text-xs">{plan.network}</span>
              </div>
              <div className="text-muted-foreground font-mono text-xs">{plan.code}</div>
            </TableCell>
            <TableCell>{label(plan.brand)}</TableCell>
            <TableCell>
              <div>{label(plan.category)}</div>
              <div className="text-muted-foreground text-xs">{label(plan.segment)}</div>
            </TableCell>
            <TableCell className="text-right">{formatMoney(plan.monthly_price)}</TableCell>
            <TableCell className="text-right">{formatLimit(plan.data_limit_gb, " GB")}</TableCell>
            <TableCell className="text-right whitespace-nowrap">
              {formatLimit(plan.minutes_limit, " min")} / {formatLimit(plan.sms_limit)}
            </TableCell>
            <TableCell className="text-right">
              {plan.contract_months > 0 ? `${plan.contract_months} mths` : "None"}
            </TableCell>
            <TableCell className="text-right">{plan.line_count.toLocaleString()}</TableCell>
            <TableCell>
              <StatusBadge value={plan.is_active ? "ACTIVE" : "INACTIVE"} />
            </TableCell>
            <TableCell className="text-right">
              <div className="flex justify-end gap-1">
                <Button
                  variant="ghost"
                  size="icon"
                  aria-label={`Edit ${plan.name}`}
                  onClick={() => onEdit(plan)}
                >
                  <Pencil className="h-4 w-4" />
                </Button>
                <Button
                  variant="ghost"
                  size="icon"
                  aria-label={`Delete ${plan.name}`}
                  onClick={() => onDelete(plan)}
                >
                  <Trash2 className="h-4 w-4" />
                </Button>
              </div>
            </TableCell>
          </TableRow>
        ))}
      </TableBody>
    </Table>
  )
}
