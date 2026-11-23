import { render, screen } from "@testing-library/react"
import { describe, it, expect, vi } from "vitest"
import { PlansTable } from "@/components/plans-table"
import type { Plan } from "@/hooks/use-plans"

const plans: Plan[] = [
  {
    id: 1,
    code: "XO-5G-100",
    name: "XO Plus 100GB",
    brand: "SINGTEL",
    category: "POSTPAID",
    segment: "CONSUMER",
    monthly_price: 75,
    data_limit_gb: 100,
    minutes_limit: 1000,
    sms_limit: 1000,
    contract_months: 24,
    network: "5G",
    is_active: true,
    line_count: 1234,
    created_at: null,
  },
  {
    id: 2,
    code: "HI-TOURIST-14",
    name: "hi!Tourist 14 days",
    brand: "HI_CARD",
    category: "PREPAID",
    segment: "CONSUMER",
    monthly_price: 15.5,
    data_limit_gb: null,
    minutes_limit: null,
    sms_limit: null,
    contract_months: 0,
    network: "4G",
    is_active: false,
    line_count: 0,
    created_at: null,
  },
]

describe("PlansTable", () => {
  it("renders the expected column headers", () => {
    render(<PlansTable plans={plans} onEdit={vi.fn()} onDelete={vi.fn()} />)

    const headers = screen.getAllByRole("columnheader").map((cell) => cell.textContent)
    expect(headers).toEqual([
      "Plan",
      "Brand",
      "Category",
      "Price/mo",
      "Data",
      "Talktime / SMS",
      "Contract",
      "Lines",
      "Status",
      "Actions",
    ])
  })

  it("shows brand and category with their product names", () => {
    render(<PlansTable plans={plans} onEdit={vi.fn()} onDelete={vi.fn()} />)

    expect(screen.getByText("Singtel")).toBeInTheDocument()
    expect(screen.getByText("hi!Card")).toBeInTheDocument()
    expect(screen.getByText("Postpaid")).toBeInTheDocument()
    expect(screen.getByText("Prepaid")).toBeInTheDocument()
  })

  it("formats prices in Singapore dollars", () => {
    render(<PlansTable plans={plans} onEdit={vi.fn()} onDelete={vi.fn()} />)

    expect(screen.getByText("S$75.00")).toBeInTheDocument()
    expect(screen.getByText("S$15.50")).toBeInTheDocument()
  })

  it("labels null limits as Unlimited and shows contract length", () => {
    render(<PlansTable plans={plans} onEdit={vi.fn()} onDelete={vi.fn()} />)

    expect(screen.getByText("100 GB")).toBeInTheDocument()
    expect(screen.getByText("1,000 min / 1,000")).toBeInTheDocument()
    expect(screen.getByText("Unlimited / Unlimited")).toBeInTheDocument()
    expect(screen.getAllByText("Unlimited")).toHaveLength(1)
    expect(screen.getByText("24 mths")).toBeInTheDocument()
    expect(screen.getByText("None")).toBeInTheDocument()
    expect(screen.getByText("1,234")).toBeInTheDocument()
  })

  it("shows an Active or Inactive badge per plan", () => {
    render(<PlansTable plans={plans} onEdit={vi.fn()} onDelete={vi.fn()} />)

    expect(screen.getByText("Active")).toBeInTheDocument()
    expect(screen.getByText("Inactive")).toBeInTheDocument()
  })

  it("calls onEdit and onDelete with the row's plan", () => {
    const onEdit = vi.fn()
    const onDelete = vi.fn()
    render(<PlansTable plans={plans} onEdit={onEdit} onDelete={onDelete} />)

    screen.getByRole("button", { name: "Edit XO Plus 100GB" }).click()
    screen.getByRole("button", { name: "Delete hi!Tourist 14 days" }).click()

    expect(onEdit).toHaveBeenCalledWith(plans[0])
    expect(onDelete).toHaveBeenCalledWith(plans[1])
  })
})
