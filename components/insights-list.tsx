"use client"

import { useId, useState } from "react"
import { ChevronDown, ChevronRight } from "lucide-react"
import { Button } from "@/components/ui/button"
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table"
import { StatusBadge, type Tone } from "@/components/status-badge"
import { ALERT_SEVERITIES, type AlertSeverity, type InsightAlert } from "@/hooks/use-insights"
import { formatCount, formatDate, formatMoney, formatMsisdn } from "@/lib/format"
import { label } from "@/lib/labels"

const SEVERITY_TONES: Record<AlertSeverity, Tone> = { HIGH: "red", MEDIUM: "orange", LOW: "gray" }

function AffectedLines({ alert, showCustomer }: { alert: InsightAlert; showCustomer: boolean }) {
  const lines = alert.lines ?? []
  const showNote = lines.some((line) => line.note)
  return (
    <div className="max-h-80 overflow-auto rounded-md border">
      <Table aria-label={`Lines for: ${alert.title}`}>
        <TableHeader className="bg-background sticky top-0 z-10">
          <TableRow>
            <TableHead>Mobile number</TableHead>
            {showCustomer && <TableHead>Customer</TableHead>}
            <TableHead>Plan</TableHead>
            <TableHead className={alert.value_kind === "AMOUNT" ? "text-right" : undefined}>
              {alert.value_label}
            </TableHead>
            {showNote && <TableHead>Details</TableHead>}
          </TableRow>
        </TableHeader>
        <TableBody>
          {lines.map((line) => (
            <TableRow key={line.line_id}>
              <TableCell className="font-mono text-sm whitespace-nowrap">
                {formatMsisdn(line.msisdn)}
              </TableCell>
              {showCustomer && <TableCell>{line.customer_name ?? "-"}</TableCell>}
              <TableCell>{line.plan_name}</TableCell>
              {alert.value_kind === "AMOUNT" ? (
                <TableCell className="text-right">{formatMoney(line.amount)}</TableCell>
              ) : (
                <TableCell className="whitespace-nowrap">{formatDate(line.date)}</TableCell>
              )}
              {showNote && <TableCell>{line.note ?? "-"}</TableCell>}
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </div>
  )
}

function AlertItem({ alert, showCustomer }: { alert: InsightAlert; showCustomer: boolean }) {
  const [open, setOpen] = useState(false)
  const panelId = useId()
  const affected = `${formatCount(alert.affected)} ${alert.affected === 1 ? "line" : "lines"}`
  return (
    <li
      aria-label={alert.title}
      className="bg-card space-y-3 rounded-xl border p-4 shadow-sm"
      data-severity={alert.severity}
    >
      <div className="flex flex-wrap items-start justify-between gap-2">
        <div className="space-y-1">
          <div className="flex items-center gap-2">
            <StatusBadge value={alert.severity} tone={SEVERITY_TONES[alert.severity]} />
            <StatusBadge value={alert.area} tone="blue" />
          </div>
          <h3 className="font-semibold">{alert.title}</h3>
          <p className="text-body-small text-muted-foreground">{alert.detail}</p>
        </div>
        {alert.lines ? (
          <Button
            variant="outline"
            size="sm"
            aria-expanded={open}
            aria-controls={panelId}
            onClick={() => setOpen((value) => !value)}
          >
            {open ? <ChevronDown /> : <ChevronRight />}
            {open ? `Hide ${affected}` : `Show ${affected}`}
          </Button>
        ) : (
          <span className="text-body-small text-muted-foreground">{affected}</span>
        )}
      </div>
      {!alert.lines && (
        <p className="text-muted-foreground text-xs">
          Your role can see how many lines this affects, but not which lines.
        </p>
      )}
      {alert.lines && open && (
        <div id={panelId}>
          <AffectedLines alert={alert} showCustomer={showCustomer} />
        </div>
      )}
    </li>
  )
}

/** Alerts in the order given (most severe first), each with its affected lines on demand. */
export function InsightsList({
  alerts,
  showCustomer = true,
}: {
  alerts: InsightAlert[]
  /** Set to false for staff who may not read customers. */
  showCustomer?: boolean
}) {
  return (
    <ul aria-label="Alerts" className="space-y-3">
      {alerts.map((alert) => (
        <AlertItem key={alert.id} alert={alert} showCustomer={showCustomer} />
      ))}
    </ul>
  )
}

/** "2 high, 3 medium, 1 low", leaving out severities with no alerts. */
export function severitySummary(alerts: InsightAlert[]): string {
  return ALERT_SEVERITIES.map((severity) => ({
    severity,
    count: alerts.filter((alert) => alert.severity === severity).length,
  }))
    .filter(({ count }) => count > 0)
    .map(({ severity, count }) => `${formatCount(count)} ${label(severity).toLowerCase()}`)
    .join(", ")
}
