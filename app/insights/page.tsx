"use client"

import { useState } from "react"
import { Loader2, RefreshCw } from "lucide-react"
import { useAuth } from "@/components/auth-provider"
import { Button } from "@/components/ui/button"
import { FilterSelect } from "@/components/filter-select"
import { InsightsList, severitySummary } from "@/components/insights-list"
import { PageShell } from "@/components/page-shell"
import {
  ALERT_AREAS,
  ALERT_SEVERITIES,
  filterAlerts,
  useInsights,
  type AlertArea,
  type AlertSeverity,
} from "@/hooks/use-insights"
import { formatDateTime } from "@/lib/format"
import { options } from "@/lib/labels"

const TITLE = "Alerts & Insights"

export default function InsightsPage() {
  const showCustomer = useAuth().can("CUSTOMER_READ")
  const { insights, loading, error, refresh } = useInsights()
  const [area, setArea] = useState<AlertArea | null>(null)
  const [severity, setSeverity] = useState<AlertSeverity | null>(null)

  if (!insights) {
    return (
      <PageShell title={TITLE} fitToWindow={false}>
        <div className="flex h-[60vh] items-center justify-center">
          {loading ? (
            <div className="text-muted-foreground flex items-center">
              <Loader2 className="mr-2 h-6 w-6 animate-spin" />
              Loading alerts...
            </div>
          ) : (
            <div role="alert" className="text-destructive">
              {error}
            </div>
          )}
        </div>
      </PageShell>
    )
  }

  const shown = filterAlerts(insights.alerts, area, severity)
  const summary = severitySummary(insights.alerts)

  return (
    <PageShell title={TITLE} fitToWindow={false}>
      <div className="flex flex-wrap items-center justify-between gap-2">
        <div className="text-body-small text-muted-foreground space-y-1">
          <p>
            Checked {formatDateTime(insights.as_of)} Singapore time.{" "}
            {summary ? `Alerts: ${summary}.` : "Nothing needs attention."}
          </p>
          {insights.usage_as_of && (
            <p>
              Usage checks cover the 7 days up to the latest usage received,{" "}
              {formatDateTime(insights.usage_as_of)}.
            </p>
          )}
        </div>
        <div className="flex items-center gap-2">
          {error && (
            <span role="alert" className="text-destructive text-sm">
              {error}
            </span>
          )}
          <Button variant="outline" size="sm" onClick={() => void refresh()} disabled={loading}>
            <RefreshCw className={loading ? "animate-spin" : undefined} />
            Refresh
          </Button>
        </div>
      </div>

      <div className="flex flex-wrap items-end gap-4">
        <FilterSelect
          id="area-filter"
          label="Area"
          value={area}
          options={options(ALERT_AREAS)}
          onChange={(v) => setArea(v as AlertArea | null)}
          className="w-[140px]"
        />
        <FilterSelect
          id="severity-filter"
          label="Severity"
          value={severity}
          options={options(ALERT_SEVERITIES)}
          onChange={(v) => setSeverity(v as AlertSeverity | null)}
          className="w-[140px]"
        />
        <p className="text-body-small text-muted-foreground pb-2">
          {shown.length} of {insights.alerts.length} alerts
        </p>
      </div>

      {shown.length > 0 ? (
        <InsightsList alerts={shown} showCustomer={showCustomer} />
      ) : (
        <p className="text-muted-foreground py-12 text-center">
          {insights.alerts.length > 0 ? "No alerts match these filters." : "No alerts right now."}
        </p>
      )}
    </PageShell>
  )
}
