import { render, screen } from "@testing-library/react"
import { describe, it, expect, vi } from "vitest"
import { CasesTable } from "@/components/cases-table"
import type { SupportCase } from "@/hooks/use-cases"

const base: SupportCase = {
  id: 1,
  case_number: "CS-000001",
  customer_id: 1,
  customer_number: "C1000001",
  customer_name: "Tan Wei Ming",
  line_id: 5,
  msisdn: "91234567",
  category: "SCAM_REPORT",
  channel: "PHONE",
  subject: "Received a suspicious SMS",
  description: null,
  priority: "URGENT",
  status: "OPEN",
  sla_due_at: "2026-03-01T04:00:00Z",
  sla_breached: false,
  created_at: "2026-03-01T00:00:00Z",
  resolved_at: null,
}

describe("CasesTable", () => {
  it("shows the customer, line and readable category", () => {
    render(<CasesTable cases={[base]} onEdit={vi.fn()} />)

    expect(screen.getByText("Tan Wei Ming")).toBeInTheDocument()
    expect(screen.getByText("C1000001 · 9123 4567")).toBeInTheDocument()
    expect(screen.getByText("Scam report · via Phone")).toBeInTheDocument()
    expect(screen.getByText("Urgent")).toBeInTheDocument()
  })

  it("flags breached SLAs and hides the SLA for finished cases", () => {
    const cases: SupportCase[] = [
      { ...base, id: 2, case_number: "CS-000002", sla_breached: true },
      { ...base, id: 3, case_number: "CS-000003", status: "RESOLVED", sla_breached: true },
    ]
    render(<CasesTable cases={cases} onEdit={vi.fn()} />)

    expect(screen.getAllByText("Breached")).toHaveLength(1)
  })

  it("does not allow closed cases to be edited", () => {
    const onEdit = vi.fn()
    render(
      <CasesTable
        cases={[base, { ...base, id: 4, case_number: "CS-000004", status: "CLOSED" }]}
        onEdit={onEdit}
      />
    )

    expect(screen.getByRole("button", { name: "Edit case CS-000004" })).toBeDisabled()
    screen.getByRole("button", { name: "Edit case CS-000001" }).click()
    expect(onEdit).toHaveBeenCalledWith(base)
  })
})
