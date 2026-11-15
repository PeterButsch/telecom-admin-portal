import { fireEvent, render, screen, waitFor } from "@testing-library/react"
import { describe, it, expect, vi } from "vitest"
import { CustomerFormDialog } from "@/components/customer-form-dialog"
import { ApiError } from "@/lib/api"
import type { Customer } from "@/hooks/use-customers"
import type { Plan } from "@/hooks/use-plans"

// The plan select is required, so the form only submits when the current plan is a listed option.
const plans: Plan[] = [
  {
    id: 2,
    name: "Standard",
    monthly_price: 30,
    data_limit_gb: 20,
    minutes_limit: null,
    sms_limit: null,
    is_active: true,
  },
]

const customer: Customer = {
  id: 1,
  first_name: "Ada",
  last_name: "Lovelace",
  email: "ada@example.com",
  phone: "555-0100",
  plan_id: 2,
  plan_name: "Standard",
  status: "ACTIVE",
  balance: 0,
  activated_at: null,
  created_at: null,
}

describe("CustomerFormDialog", () => {
  it("keeps the dialog open and shows the server message when saving fails", async () => {
    const onSubmit = vi
      .fn()
      .mockRejectedValue(new ApiError("A customer with this email already exists.", 409))
    const onOpenChange = vi.fn()
    render(
      <CustomerFormDialog
        open
        onOpenChange={onOpenChange}
        customer={customer}
        plans={plans}
        onSubmit={onSubmit}
      />
    )

    fireEvent.click(screen.getByRole("button", { name: "Save Changes" }))

    expect(await screen.findByRole("alert")).toHaveTextContent(
      "A customer with this email already exists."
    )
    expect(onOpenChange).not.toHaveBeenCalledWith(false)
    expect(screen.getByRole("button", { name: "Save Changes" })).toBeEnabled()
  })

  it("closes after a successful save", async () => {
    const onSubmit = vi.fn().mockResolvedValue(undefined)
    const onOpenChange = vi.fn()
    render(
      <CustomerFormDialog
        open
        onOpenChange={onOpenChange}
        customer={customer}
        plans={plans}
        onSubmit={onSubmit}
      />
    )

    fireEvent.click(screen.getByRole("button", { name: "Save Changes" }))

    await waitFor(() => expect(onOpenChange).toHaveBeenCalledWith(false))
    expect(onSubmit).toHaveBeenCalledWith(
      expect.objectContaining({ email: "ada@example.com", status: "ACTIVE" })
    )
  })
})
