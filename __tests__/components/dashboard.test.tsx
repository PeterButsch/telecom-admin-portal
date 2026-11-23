import { render, screen, within } from "@testing-library/react"
import { describe, expect, it } from "vitest"
import { DashboardKpis } from "@/components/dashboard-kpis"
import { BrandTable, SegmentComparison } from "@/components/dashboard-tables"
import { overviewFixture } from "../fixtures/dashboard-overview"

function kpi(name: string) {
  return within(screen.getByRole("group", { name }))
}

describe("DashboardKpis", () => {
  it("shows revenue, lines, churn, porting and SLA figures", () => {
    render(<DashboardKpis kpis={overviewFixture.kpis} />)

    expect(kpi("Monthly recurring revenue").getByText("S$156.00")).toBeInTheDocument()
    expect(kpi("Monthly recurring revenue").getByText("ARPU S$31.20 per active line")).toBeVisible()
    expect(kpi("Active lines").getByText("5")).toBeInTheDocument()
    expect(kpi("Active lines").getByText("+3 net in the last 30 days")).toBeInTheDocument()
    expect(kpi("Churn, last 30 days").getByText("100.00%")).toBeInTheDocument()
    expect(kpi("Net porting, last 30 days").getByText("-1")).toBeInTheDocument()
    expect(kpi("Net porting, last 30 days").getByText("1 ported in, 2 ported out")).toBeVisible()
    expect(kpi("Open cases").getByText("1 past their SLA")).toHaveClass("text-destructive")
    expect(kpi("SLA met, last 30 days").getByText("50.0%")).toBeInTheDocument()
  })

  it("shows a dash for rates with nothing to divide by", () => {
    render(
      <DashboardKpis
        kpis={{
          ...overviewFixture.kpis,
          arpu: null,
          churn_rate_30d: null,
          sla_met_rate_30d: null,
          breached_open_cases: 0,
        }}
      />
    )

    expect(kpi("Churn, last 30 days").getByText("-")).toBeInTheDocument()
    expect(kpi("SLA met, last 30 days").getByText("-")).toBeInTheDocument()
    expect(kpi("Monthly recurring revenue").getByText("ARPU - per active line")).toBeVisible()
    expect(kpi("Open cases").getByText("0 past their SLA")).not.toHaveClass("text-destructive")
  })
})

describe("BrandTable", () => {
  it("lists every brand with its lines, revenue, ARPU and share", () => {
    render(<BrandTable brands={overviewFixture.brands} />)

    const rows = screen.getAllByRole("row").slice(1)
    expect(rows.map((row) => row.textContent)).toEqual([
      "Singtel3S$120.00S$40.0076.9%",
      "GOMO0S$0.00-0.0%",
      "hi!Card2S$36.00S$18.0023.1%",
    ])
  })
})

describe("SegmentComparison", () => {
  it("compares consumer and enterprise customers", () => {
    render(<SegmentComparison segments={overviewFixture.segments} />)

    const rows = screen.getAllByRole("row").slice(1)
    expect(rows.map((row) => row.textContent)).toEqual([
      "Consumer14S$116.00S$29.00",
      "Enterprise11S$40.00S$40.00",
    ])
    expect(screen.getByText("Consumer 74.4% of MRR")).toBeVisible()
    expect(screen.getByText("Enterprise 25.6% of MRR")).toBeVisible()
  })
})
