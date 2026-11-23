"use client"

import {
  ArrowLeftRight,
  Clock,
  DollarSign,
  LifeBuoy,
  Signal,
  TrendingDown,
  type LucideIcon,
} from "lucide-react"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import type { OverviewKpis } from "@/hooks/use-dashboard-overview"
import { formatCount, formatMoney, formatPercent, formatSigned } from "@/lib/format"
import { cn } from "@/lib/utils"

interface KpiProps {
  title: string
  value: string
  caption: string
  icon: LucideIcon
  warning?: boolean
}

function Kpi({ title, value, caption, icon: Icon, warning = false }: KpiProps) {
  return (
    <Card role="group" aria-label={title} className="gap-2">
      <CardHeader className="flex flex-row items-center justify-between space-y-0">
        <CardTitle className="text-body-small font-medium">{title}</CardTitle>
        <Icon aria-hidden className="text-muted-foreground h-4 w-4" />
      </CardHeader>
      <CardContent>
        <div className="text-headline-2">{value}</div>
        <p
          className={cn(
            "text-caption",
            warning ? "text-destructive font-medium" : "text-muted-foreground"
          )}
        >
          {caption}
        </p>
      </CardContent>
    </Card>
  )
}

/** Headline figures: revenue, lines, churn, porting and case SLAs. */
export function DashboardKpis({ kpis }: { kpis: OverviewKpis }) {
  return (
    <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3 2xl:grid-cols-6">
      <Kpi
        title="Monthly recurring revenue"
        value={formatMoney(kpis.monthly_revenue)}
        caption={`ARPU ${formatMoney(kpis.arpu)} per active line`}
        icon={DollarSign}
      />
      <Kpi
        title="Active lines"
        value={formatCount(kpis.active_lines)}
        caption={`${formatSigned(kpis.net_adds_30d)} net in the last 30 days`}
        icon={Signal}
      />
      <Kpi
        title="Churn, last 30 days"
        value={formatPercent(kpis.churn_rate_30d, 2)}
        caption={`${formatCount(kpis.terminations_30d)} lines terminated`}
        icon={TrendingDown}
      />
      <Kpi
        title="Net porting, last 30 days"
        value={formatSigned(kpis.port_ins_30d - kpis.port_outs_30d)}
        caption={`${formatCount(kpis.port_ins_30d)} ported in, ${formatCount(kpis.port_outs_30d)} ported out`}
        icon={ArrowLeftRight}
      />
      <Kpi
        title="Open cases"
        value={formatCount(kpis.open_cases)}
        caption={`${formatCount(kpis.breached_open_cases)} past their SLA`}
        icon={LifeBuoy}
        warning={kpis.breached_open_cases > 0}
      />
      <Kpi
        title="SLA met, last 30 days"
        value={formatPercent(kpis.sla_met_rate_30d)}
        caption={`${formatCount(kpis.resolved_30d)} cases resolved`}
        icon={Clock}
      />
    </div>
  )
}
