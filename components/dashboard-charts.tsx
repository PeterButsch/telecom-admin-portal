"use client"

import { Bar, BarChart, CartesianGrid, ComposedChart, Line, XAxis, YAxis } from "recharts"
import {
  ChartContainer,
  ChartLegend,
  ChartLegendContent,
  ChartTooltip,
  ChartTooltipContent,
  type ChartConfig,
} from "@/components/ui/chart"
import type {
  BrandFigures,
  CasePriorityFigures,
  MonthFigures,
  OperatorFigures,
} from "@/hooks/use-dashboard-overview"
import { BRAND_COLOURS } from "@/lib/dashboard"
import { formatMonth } from "@/lib/format"
import { label } from "@/lib/labels"

const money = (value: number) => `S$${value.toLocaleString("en-SG")}`

const brandConfig = {
  monthly_revenue: { label: "MRR (S$)" },
  SINGTEL: { label: "Singtel", color: BRAND_COLOURS.SINGTEL },
  GOMO: { label: "GOMO", color: BRAND_COLOURS.GOMO },
  HI_CARD: { label: "hi!Card", color: BRAND_COLOURS.HI_CARD },
} satisfies ChartConfig

export function BrandRevenueChart({ brands }: { brands: BrandFigures[] }) {
  const data = brands.map((row) => ({ ...row, fill: BRAND_COLOURS[row.brand] }))
  return (
    <ChartContainer config={brandConfig} className="h-[180px] w-full">
      <BarChart accessibilityLayer data={data} layout="vertical" margin={{ left: 8, right: 16 }}>
        <CartesianGrid horizontal={false} />
        <XAxis type="number" tickLine={false} axisLine={false} tickFormatter={money} />
        <YAxis
          dataKey="brand"
          type="category"
          tickLine={false}
          axisLine={false}
          width={64}
          tickFormatter={(value: string) => label(value)}
        />
        <ChartTooltip content={<ChartTooltipContent nameKey="brand" />} />
        <Bar dataKey="monthly_revenue" radius={4} />
      </BarChart>
    </ChartContainer>
  )
}

const movementConfig = {
  activations: { label: "Activations", color: "var(--chart-3)" },
  terminations: { label: "Terminations", color: "var(--chart-5)" },
  churn_rate: { label: "Churn (%)", color: "var(--chart-1)" },
} satisfies ChartConfig

/** Activations and terminations per month, with the monthly churn rate on its own axis. */
export function LineMovementChart({ months }: { months: MonthFigures[] }) {
  const data = months.map((row) => ({
    ...row,
    name: row.partial ? `${formatMonth(row.month)}*` : formatMonth(row.month),
  }))
  return (
    <ChartContainer config={movementConfig} className="h-[300px] w-full">
      <ComposedChart accessibilityLayer data={data} margin={{ left: 0, right: 0 }}>
        <CartesianGrid vertical={false} />
        <XAxis dataKey="name" tickLine={false} axisLine={false} tickMargin={8} />
        <YAxis yAxisId="lines" tickLine={false} axisLine={false} width={40} />
        <YAxis
          yAxisId="churn"
          orientation="right"
          tickLine={false}
          axisLine={false}
          width={48}
          tickFormatter={(value: number) => `${value}%`}
        />
        <ChartTooltip content={<ChartTooltipContent />} />
        <ChartLegend content={<ChartLegendContent />} />
        <Bar yAxisId="lines" dataKey="activations" fill="var(--color-activations)" radius={4} />
        <Bar yAxisId="lines" dataKey="terminations" fill="var(--color-terminations)" radius={4} />
        <Line
          yAxisId="churn"
          dataKey="churn_rate"
          type="linear"
          stroke="var(--color-churn_rate)"
          strokeWidth={2}
          dot={{ r: 3 }}
          connectNulls
        />
      </ComposedChart>
    </ChartContainer>
  )
}

const portingConfig = {
  port_ins: { label: "Ported in from", color: "var(--chart-1)" },
  port_outs: { label: "Ported out to", color: "var(--chart-5)" },
} satisfies ChartConfig

export function PortingChart({ porting }: { porting: OperatorFigures[] }) {
  return (
    <ChartContainer config={portingConfig} className="h-[260px] w-full">
      <BarChart accessibilityLayer data={porting} layout="vertical" margin={{ left: 8 }}>
        <CartesianGrid horizontal={false} />
        <XAxis type="number" tickLine={false} axisLine={false} allowDecimals={false} />
        <YAxis
          dataKey="operator"
          type="category"
          tickLine={false}
          axisLine={false}
          width={84}
          tickFormatter={(value: string) => label(value)}
        />
        <ChartTooltip
          content={<ChartTooltipContent labelFormatter={(value: string) => label(value)} />}
        />
        <ChartLegend content={<ChartLegendContent />} />
        <Bar dataKey="port_ins" fill="var(--color-port_ins)" radius={4} />
        <Bar dataKey="port_outs" fill="var(--color-port_outs)" radius={4} />
      </BarChart>
    </ChartContainer>
  )
}

const casesConfig = {
  within_sla: { label: "Within SLA", color: "var(--chart-3)" },
  breached: { label: "Past SLA", color: "var(--chart-1)" },
} satisfies ChartConfig

export function CaseSlaChart({ cases }: { cases: CasePriorityFigures[] }) {
  const data = cases.map((row) => ({
    priority: row.priority,
    within_sla: row.open - row.breached,
    breached: row.breached,
  }))
  return (
    <ChartContainer config={casesConfig} className="h-[260px] w-full">
      <BarChart accessibilityLayer data={data} margin={{ left: 0 }}>
        <CartesianGrid vertical={false} />
        <XAxis
          dataKey="priority"
          tickLine={false}
          axisLine={false}
          tickMargin={8}
          tickFormatter={(value: string) => label(value)}
        />
        <YAxis tickLine={false} axisLine={false} width={40} allowDecimals={false} />
        <ChartTooltip
          content={<ChartTooltipContent labelFormatter={(value: string) => label(value)} />}
        />
        <ChartLegend content={<ChartLegendContent />} />
        <Bar dataKey="within_sla" stackId="cases" fill="var(--color-within_sla)" />
        <Bar
          dataKey="breached"
          stackId="cases"
          fill="var(--color-breached)"
          radius={[4, 4, 0, 0]}
        />
      </BarChart>
    </ChartContainer>
  )
}
