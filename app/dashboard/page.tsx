"use client"

import { DollarSign, LifeBuoy, Loader2, Signal, Smartphone, Users } from "lucide-react"
import { Bar, BarChart, CartesianGrid, Pie, PieChart, XAxis, YAxis } from "recharts"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import {
  ChartContainer,
  ChartTooltip,
  ChartTooltipContent,
  type ChartConfig,
} from "@/components/ui/chart"
import { PageShell } from "@/components/page-shell"
import { useDashboardStats } from "@/hooks/use-dashboard-stats"
import { formatMoney } from "@/lib/format"
import { label } from "@/lib/labels"

const TOP_PLANS = 10
const palette = (index: number) => `var(--chart-${(index % 8) + 1})`

/** Builds a chart config that gives each category its own colour and a readable label. */
function seriesConfig(
  names: string[],
  valueLabel: string,
  valueKey = "value",
  colours: Record<string, string> = {}
): ChartConfig {
  const config: ChartConfig = { [valueKey]: { label: valueLabel } }
  names.forEach((name, i) => {
    config[name] = { label: label(name), color: colours[name] ?? palette(i) }
  })
  return config
}

function withFill<T extends { name: string }>(rows: T[], config: ChartConfig) {
  return rows.map((row) => ({ ...row, fill: config[row.name]?.color ?? palette(0) }))
}

const STATUS_COLOURS: Record<string, string> = {
  OPEN: "var(--chart-1)",
  IN_PROGRESS: "var(--chart-4)",
  PENDING_CUSTOMER: "var(--chart-2)",
  RESOLVED: "var(--chart-3)",
  CLOSED: "var(--chart-6)",
  ASSIGNED: "var(--chart-1)",
  AVAILABLE: "var(--chart-3)",
  DAMAGED: "var(--chart-4)",
  LOST: "var(--chart-5)",
}

function Kpi({
  title,
  value,
  caption,
  icon: Icon,
}: {
  title: string
  value: string
  caption: string
  icon: React.ElementType
}) {
  return (
    <Card>
      <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
        <CardTitle className="text-body-small">{title}</CardTitle>
        <Icon className="text-muted-foreground h-4 w-4" />
      </CardHeader>
      <CardContent>
        <div className="text-headline-2">{value}</div>
        <p className="text-caption text-muted-foreground">{caption}</p>
      </CardContent>
    </Card>
  )
}

export default function DashboardPage() {
  const { stats, linesByPlan, devicesByStatus, casesByStatus, revenueByPlan, loading, error } =
    useDashboardStats()

  if (loading || error) {
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

  const topLines = linesByPlan.slice(0, TOP_PLANS)
  const topRevenue = revenueByPlan.slice(0, TOP_PLANS)
  const linesConfig = seriesConfig(
    topLines.map((r) => r.name),
    "Lines"
  )
  const revenueConfig = seriesConfig(
    topRevenue.map((r) => r.name),
    "Revenue",
    "revenue"
  )
  const devicesConfig = seriesConfig(
    devicesByStatus.map((r) => r.name),
    "Devices",
    "value",
    STATUS_COLOURS
  )
  const casesConfig = seriesConfig(
    casesByStatus.map((r) => r.name),
    "Cases",
    "value",
    STATUS_COLOURS
  )

  return (
    <PageShell title="Dashboard" fitToWindow={false}>
      <div className="grid gap-4 md:grid-cols-3 lg:grid-cols-5">
        <Kpi
          title="Active Customers"
          value={(stats?.active_customers ?? 0).toLocaleString()}
          caption="Consumer and business accounts"
          icon={Users}
        />
        <Kpi
          title="Active Lines"
          value={(stats?.active_lines ?? 0).toLocaleString()}
          caption="Mobile lines in service"
          icon={Signal}
        />
        <Kpi
          title="Monthly Recurring Revenue"
          value={formatMoney(stats?.monthly_revenue ?? 0)}
          caption="Plan charges on active lines"
          icon={DollarSign}
        />
        <Kpi
          title="Open Cases"
          value={(stats?.open_cases ?? 0).toLocaleString()}
          caption="Not yet resolved"
          icon={LifeBuoy}
        />
        <Kpi
          title="Devices in Use"
          value={(stats?.devices_in_use ?? 0).toLocaleString()}
          caption="Assigned to a line"
          icon={Smartphone}
        />
      </div>

      <div className="grid gap-4 md:grid-cols-2">
        <Card>
          <CardHeader>
            <CardTitle className="text-headline-3">
              Active Lines by Plan (top {TOP_PLANS})
            </CardTitle>
          </CardHeader>
          <CardContent>
            <ChartContainer config={linesConfig} className="h-[300px] w-full">
              <BarChart accessibilityLayer data={withFill(topLines, linesConfig)} layout="vertical">
                <CartesianGrid horizontal={false} />
                <XAxis type="number" tickLine={false} axisLine={false} />
                <YAxis
                  dataKey="name"
                  type="category"
                  tickLine={false}
                  axisLine={false}
                  width={140}
                />
                <ChartTooltip content={<ChartTooltipContent nameKey="name" />} />
                <Bar dataKey="value" radius={4} />
              </BarChart>
            </ChartContainer>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle className="text-headline-3">
              Monthly Revenue by Plan (top {TOP_PLANS})
            </CardTitle>
          </CardHeader>
          <CardContent>
            <ChartContainer config={revenueConfig} className="h-[300px] w-full">
              <BarChart
                accessibilityLayer
                data={withFill(topRevenue, revenueConfig)}
                layout="vertical"
              >
                <CartesianGrid horizontal={false} />
                <XAxis
                  type="number"
                  tickLine={false}
                  axisLine={false}
                  tickFormatter={(value: number) => `S$${value.toLocaleString()}`}
                />
                <YAxis
                  dataKey="name"
                  type="category"
                  tickLine={false}
                  axisLine={false}
                  width={140}
                />
                <ChartTooltip content={<ChartTooltipContent nameKey="name" />} />
                <Bar dataKey="revenue" radius={4} />
              </BarChart>
            </ChartContainer>
          </CardContent>
        </Card>
      </div>

      <div className="grid gap-4 md:grid-cols-2">
        <Card>
          <CardHeader>
            <CardTitle className="text-headline-3">Device Status</CardTitle>
          </CardHeader>
          <CardContent>
            <ChartContainer config={devicesConfig} className="h-[250px] w-full">
              <BarChart
                accessibilityLayer
                data={withFill(devicesByStatus, devicesConfig)}
                layout="vertical"
              >
                <CartesianGrid horizontal={false} />
                <XAxis type="number" tickLine={false} axisLine={false} />
                <YAxis
                  dataKey="name"
                  type="category"
                  tickLine={false}
                  axisLine={false}
                  tickFormatter={(value: string) => label(value)}
                  width={85}
                />
                <ChartTooltip content={<ChartTooltipContent nameKey="name" />} />
                <Bar dataKey="value" radius={4} />
              </BarChart>
            </ChartContainer>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle className="text-headline-3">Case Status</CardTitle>
          </CardHeader>
          <CardContent>
            <ChartContainer config={casesConfig} className="mx-auto h-[250px] w-full">
              <PieChart accessibilityLayer>
                <ChartTooltip
                  cursor={false}
                  content={<ChartTooltipContent hideLabel nameKey="name" />}
                />
                <Pie
                  data={withFill(casesByStatus, casesConfig)}
                  dataKey="value"
                  nameKey="name"
                  innerRadius={60}
                  outerRadius={100}
                />
              </PieChart>
            </ChartContainer>
          </CardContent>
        </Card>
      </div>
    </PageShell>
  )
}
