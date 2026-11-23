"use client"

import { Loader2, RefreshCw } from "lucide-react"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import {
  BrandRevenueChart,
  CaseSlaChart,
  LineMovementChart,
  PortingChart,
} from "@/components/dashboard-charts"
import { DashboardKpis } from "@/components/dashboard-kpis"
import { BrandTable, SegmentComparison } from "@/components/dashboard-tables"
import { PageShell } from "@/components/page-shell"
import { useDashboardOverview } from "@/hooks/use-dashboard-overview"
import { formatCount, formatDateTime, formatSigned } from "@/lib/format"

function Section({
  title,
  description,
  children,
}: {
  title: string
  description: string
  children: React.ReactNode
}) {
  return (
    <Card role="region" aria-label={title}>
      <CardHeader>
        <CardTitle className="text-headline-3">{title}</CardTitle>
        <CardDescription>{description}</CardDescription>
      </CardHeader>
      <CardContent className="space-y-4">{children}</CardContent>
    </Card>
  )
}

export default function DashboardPage() {
  const { overview, loading, error, refresh } = useDashboardOverview()

  if (!overview) {
    return (
      <PageShell title="Dashboard" fitToWindow={false}>
        <div className="flex h-[60vh] items-center justify-center">
          {loading ? (
            <div className="text-muted-foreground flex items-center">
              <Loader2 className="mr-2 h-6 w-6 animate-spin" />
              Loading dashboard...
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

  const { kpis, brands, segments, months, porting, open_cases: cases } = overview
  const portIns = porting.reduce((sum, row) => sum + row.port_ins, 0)
  const portOuts = porting.reduce((sum, row) => sum + row.port_outs, 0)

  return (
    <PageShell title="Dashboard" fitToWindow={false}>
      <div className="flex flex-wrap items-center justify-between gap-2">
        <p className="text-body-small text-muted-foreground">
          Figures as at {formatDateTime(overview.as_of)} Singapore time. Revenue is the monthly plan
          price of every active line.
        </p>
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

      <DashboardKpis kpis={kpis} />

      <div className="grid gap-4 xl:grid-cols-2">
        <Section
          title="Revenue and ARPU by brand"
          description="Monthly recurring revenue (MRR) and average revenue per active line (ARPU)."
        >
          <BrandRevenueChart brands={brands} />
          <BrandTable brands={brands} />
        </Section>
        <Section
          title="Consumer and enterprise"
          description={`Individual and business customers. ${formatCount(kpis.active_customers)} active customers in total.`}
        >
          <SegmentComparison segments={segments} />
        </Section>
      </div>

      <Section
        title="Line movement and churn"
        description="Activations, terminations and churn (terminations as a share of lines in service at the start of the month) for the last 12 months. * Month to date."
      >
        <LineMovementChart months={months} />
      </Section>

      <div className="grid gap-4 xl:grid-cols-2">
        <Section
          title="Porting by operator"
          description={`Numbers ported in from and out to other operators in the last 12 months: ${formatCount(portIns)} in, ${formatCount(portOuts)} out (net ${formatSigned(portIns - portOuts)}).`}
        >
          <PortingChart porting={porting} />
        </Section>
        <Section
          title="Open cases by priority"
          description={`${formatCount(kpis.open_cases)} open cases, ${formatCount(kpis.breached_open_cases)} past their SLA due time.`}
        >
          <CaseSlaChart cases={cases} />
        </Section>
      </div>
    </PageShell>
  )
}
