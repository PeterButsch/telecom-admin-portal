import { render, screen } from "@testing-library/react"
import { describe, it, expect, vi } from "vitest"
import { CustomersTable } from "@/components/customers-table"
import type { Customer } from "@/hooks/use-customers"

const individual: Customer = {
  id: 1,
  customer_number: "C1000001",
  type: "INDIVIDUAL",
  display_name: "Tan Wei Ming",
  first_name: "Wei Ming",
  last_name: "Tan",
  company_name: null,
  uen: null,
  contact_name: null,
  id_type: "NRIC",
  id_number_masked: "*****567D",
  date_of_birth: null,
  email: "weiming.tan@example.com",
  phone: "9123 4567",
  address_line: null,
  postal_code: null,
  status: "ACTIVE",
  account_count: 1,
  line_count: 3,
  active_line_count: 2,
  total_balance: 12.3,
  created_at: "2026-01-01T00:00:00Z",
}

const business: Customer = {
  ...individual,
  id: 2,
  customer_number: "C1000002",
  type: "BUSINESS",
  display_name: "Lion City Logistics Pte Ltd",
  first_name: null,
  last_name: null,
  company_name: "Lion City Logistics Pte Ltd",
  uen: "201912345K",
  contact_name: "Priya Nair",
  id_type: null,
  id_number_masked: null,
  status: "SUSPENDED",
  total_balance: -5,
}

const customers = [individual, business]

describe("CustomersTable", () => {
  it("renders the expected column headers", () => {
    render(<CustomersTable customers={customers} onEdit={vi.fn()} onDelete={vi.fn()} />)

    const headers = screen.getAllByRole("columnheader").map((cell) => cell.textContent)
    expect(headers).toEqual([
      "Customer no.",
      "Name",
      "ID",
      "Contact",
      "Lines",
      "Status",
      "Balance",
      "Actions",
    ])
  })

  it("shows the masked NRIC for individuals and the UEN for businesses", () => {
    render(<CustomersTable customers={customers} onEdit={vi.fn()} onDelete={vi.fn()} />)

    expect(screen.getByText("NRIC *****567D")).toBeInTheDocument()
    expect(screen.getByText("UEN 201912345K")).toBeInTheDocument()
    expect(screen.getByText("Priya Nair")).toBeInTheDocument()
  })

  it("formats balances in Singapore dollars", () => {
    render(<CustomersTable customers={customers} onEdit={vi.fn()} onDelete={vi.fn()} />)

    expect(screen.getByText("S$12.30")).toBeInTheDocument()
    expect(screen.getByText("-S$5.00")).toBeInTheDocument()
  })

  it("shows active and total line counts", () => {
    render(<CustomersTable customers={customers} onEdit={vi.fn()} onDelete={vi.fn()} />)

    expect(screen.getAllByText("2 / 3")).toHaveLength(2)
  })

  it("renders readable status badges", () => {
    render(<CustomersTable customers={customers} onEdit={vi.fn()} onDelete={vi.fn()} />)

    expect(screen.getByText("Active")).toBeInTheDocument()
    expect(screen.getByText("Suspended")).toBeInTheDocument()
  })

  it("hides the actions the user may not take", () => {
    render(<CustomersTable customers={customers} onEdit={vi.fn()} />)

    expect(screen.getByRole("button", { name: "Edit Tan Wei Ming" })).toBeInTheDocument()
    expect(screen.queryByRole("button", { name: /^Delete / })).not.toBeInTheDocument()
  })

  it("drops the actions column for read-only users", () => {
    render(<CustomersTable customers={customers} />)

    expect(screen.queryByRole("columnheader", { name: "Actions" })).not.toBeInTheDocument()
    expect(screen.queryByRole("button")).not.toBeInTheDocument()
  })

  it("calls onEdit and onDelete with the row's customer", () => {
    const onEdit = vi.fn()
    const onDelete = vi.fn()
    render(<CustomersTable customers={customers} onEdit={onEdit} onDelete={onDelete} />)

    screen.getByRole("button", { name: "Edit Tan Wei Ming" }).click()
    screen.getByRole("button", { name: "Delete Lion City Logistics Pte Ltd" }).click()

    expect(onEdit).toHaveBeenCalledWith(individual)
    expect(onDelete).toHaveBeenCalledWith(business)
  })
})
