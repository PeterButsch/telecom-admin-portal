import { fireEvent, render, screen, within } from "@testing-library/react"
import { describe, expect, it } from "vitest"
import { InsightsList, severitySummary } from "@/components/insights-list"
import { filterAlerts, type InsightAlert } from "@/hooks/use-insights"

const roaming: InsightAlert = {
  id: "roaming-high-1",
  area: "USAGE",
  severity: "HIGH",
  check: "ROAMING_CHARGES",
  title: "S$95.95 roaming charges on 9123 4567 in 7 days",
  detail: "Possible bill shock.",
  affected: 1,
  value_label: "Roaming, 7 days",
  value_kind: "AMOUNT",
  lines: [
    {
      line_id: 1,
      msisdn: "91234567",
      customer_id: 1,
      customer_name: "Ada Tan",
      plan_name: "Singtel 5G 100GB",
      amount: 95.95,
      date: null,
      note: "Data S$80.00, calls S$15.95",
    },
  ],
}

const contracts: InsightAlert = {
  id: "contracts-ending",
  area: "PLANS",
  severity: "MEDIUM",
  check: "CONTRACT_ENDING",
  title: "2 contracts end in the next 30 days",
  detail: "Offer a re-contract.",
  affected: 2,
  value_label: "Contract ends",
  value_kind: "DATE",
  lines: [
    {
      line_id: 2,
      msisdn: "81234567",
      customer_id: 2,
      customer_name: "Lion City Logistics Pte. Ltd.",
      plan_name: "Business 5G",
      amount: null,
      date: "2026-03-22",
      note: null,
    },
    {
      line_id: 3,
      msisdn: "98765432",
      customer_id: 2,
      customer_name: "Lion City Logistics Pte. Ltd.",
      plan_name: "Business 5G",
      amount: null,
      date: "2026-03-23",
      note: null,
    },
  ],
}

const outOfContract: InsightAlert = {
  ...contracts,
  id: "out-of-contract",
  severity: "LOW",
  check: "OUT_OF_CONTRACT",
  title: "220 active lines are out of contract",
  affected: 220,
  value_label: "Contract ended",
  lines: null,
}

describe("InsightsList", () => {
  it("lists alerts with their severity, area and explanation", () => {
    render(<InsightsList alerts={[roaming, contracts]} />)

    const items = within(screen.getByRole("list", { name: "Alerts" })).getAllByRole("listitem")
    expect(items).toHaveLength(2)
    expect(within(items[0]).getByText("High")).toBeInTheDocument()
    expect(within(items[0]).getByText("Usage")).toBeInTheDocument()
    expect(within(items[0]).getByText("Possible bill shock.")).toBeInTheDocument()
    expect(within(items[1]).getByText("Plans")).toBeInTheDocument()
  })

  it("shows the affected lines on request", () => {
    render(<InsightsList alerts={[roaming, contracts]} />)

    expect(screen.queryByRole("table")).not.toBeInTheDocument()
    fireEvent.click(screen.getByRole("button", { name: "Show 1 line" }))
    const table = screen.getByRole("table", { name: `Lines for: ${roaming.title}` })
    expect(within(table).getAllByRole("row")[1]?.textContent).toBe(
      "9123 4567Ada TanSingtel 5G 100GBS$95.95Data S$80.00, calls S$15.95"
    )
    expect(screen.getByRole("button", { name: "Hide 1 line" })).toHaveAttribute(
      "aria-expanded",
      "true"
    )

    fireEvent.click(screen.getByRole("button", { name: "Show 2 lines" }))
    const dates = screen.getByRole("table", { name: `Lines for: ${contracts.title}` })
    expect(within(dates).getByRole("columnheader", { name: "Contract ends" })).toBeInTheDocument()
    expect(within(dates).queryByRole("columnheader", { name: "Details" })).not.toBeInTheDocument()
    expect(within(dates).getByText("22 Mar 2026")).toBeInTheDocument()
  })

  it("leaves out customers for staff who may not read them", () => {
    render(<InsightsList alerts={[roaming]} showCustomer={false} />)

    fireEvent.click(screen.getByRole("button", { name: "Show 1 line" }))
    expect(screen.queryByRole("columnheader", { name: "Customer" })).not.toBeInTheDocument()
    expect(screen.queryByText("Ada Tan")).not.toBeInTheDocument()
  })

  it("only gives the count when the role may not see lines", () => {
    render(<InsightsList alerts={[outOfContract]} />)

    expect(screen.queryByRole("button")).not.toBeInTheDocument()
    expect(screen.getByText("220 lines")).toBeInTheDocument()
    expect(screen.getByText(/can see how many lines this affects/)).toBeInTheDocument()
  })
})

describe("alert filters and summary", () => {
  const alerts = [roaming, contracts, outOfContract]

  it("filters by area and severity", () => {
    expect(filterAlerts(alerts, null, null)).toEqual(alerts)
    expect(filterAlerts(alerts, "PLANS", null)).toEqual([contracts, outOfContract])
    expect(filterAlerts(alerts, "PLANS", "LOW")).toEqual([outOfContract])
    expect(filterAlerts(alerts, "USAGE", "LOW")).toEqual([])
  })

  it("counts alerts by severity, most severe first", () => {
    expect(severitySummary([outOfContract, contracts, roaming, contracts])).toBe(
      "1 high, 2 medium, 1 low"
    )
    expect(severitySummary([contracts])).toBe("1 medium")
    expect(severitySummary([])).toBe("")
  })
})
