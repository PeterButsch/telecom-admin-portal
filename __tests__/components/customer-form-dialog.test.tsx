import { fireEvent, render, screen, waitFor } from "@testing-library/react"
import { describe, it, expect, vi } from "vitest"
import { CustomerFormDialog } from "@/components/customer-form-dialog"
import { ApiError } from "@/lib/api"
import type { Customer } from "@/hooks/use-customers"

const customer: Customer = {
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
  date_of_birth: "1985-04-12",
  email: "weiming.tan@example.com",
  phone: "9123 4567",
  address_line: "Blk 123 Ang Mo Kio Ave 3 #05-12",
  postal_code: "560123",
  status: "ACTIVE",
  account_count: 1,
  line_count: 2,
  active_line_count: 2,
  total_balance: 0,
  created_at: null,
}

const business: Customer = {
  ...customer,
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
  date_of_birth: null,
}

function renderForm(props: Partial<React.ComponentProps<typeof CustomerFormDialog>> = {}) {
  const onSubmit = vi.fn().mockResolvedValue(undefined)
  const onOpenChange = vi.fn()
  render(
    <CustomerFormDialog
      open
      onOpenChange={onOpenChange}
      customer={null}
      onSubmit={onSubmit}
      {...props}
    />
  )
  return { onSubmit: (props.onSubmit as typeof onSubmit) ?? onSubmit, onOpenChange }
}

describe("CustomerFormDialog", () => {
  it("keeps the dialog open and shows the server message when saving fails", async () => {
    const onSubmit = vi
      .fn()
      .mockRejectedValue(new ApiError("A customer with this email already exists.", 409))
    const { onOpenChange } = renderForm({ customer, onSubmit })

    fireEvent.click(screen.getByRole("button", { name: "Save Changes" }))

    expect(await screen.findByRole("alert")).toHaveTextContent(
      "A customer with this email already exists."
    )
    expect(onOpenChange).not.toHaveBeenCalledWith(false)
    expect(screen.getByRole("button", { name: "Save Changes" })).toBeEnabled()
  })

  it("shows field errors next to the fields the server rejected", async () => {
    const onSubmit = vi
      .fn()
      .mockRejectedValue(
        new ApiError("phone must contain 8 to 20 digits", 400, { phone: "must contain 8 digits" })
      )
    renderForm({ customer, onSubmit })

    fireEvent.click(screen.getByRole("button", { name: "Save Changes" }))

    expect(await screen.findByRole("alert")).toHaveTextContent(
      "Please correct the highlighted fields."
    )
    expect(screen.getByText("must contain 8 digits")).toBeInTheDocument()
    expect(screen.getByLabelText("Contact number")).toHaveAttribute("aria-invalid", "true")
  })

  it("sends only editable contact fields when an individual is updated", async () => {
    const { onSubmit, onOpenChange } = renderForm({ customer })

    fireEvent.change(screen.getByLabelText("Email"), {
      target: { value: "  wm.tan@example.com " },
    })
    fireEvent.click(screen.getByRole("button", { name: "Save Changes" }))

    await waitFor(() => expect(onOpenChange).toHaveBeenCalledWith(false))
    const body = onSubmit.mock.calls[0][0]
    expect(body).toEqual({
      firstName: "Wei Ming",
      lastName: "Tan",
      email: "wm.tan@example.com",
      phone: "9123 4567",
      addressLine: "Blk 123 Ang Mo Kio Ave 3 #05-12",
      postalCode: "560123",
      status: "ACTIVE",
    })
    expect(body).not.toHaveProperty("idNumber")
  })

  it("clears the postal code when it is emptied", async () => {
    const { onSubmit } = renderForm({ customer })

    fireEvent.change(screen.getByLabelText("Postal code"), { target: { value: "  " } })
    fireEvent.click(screen.getByRole("button", { name: "Save Changes" }))

    await waitFor(() => expect(onSubmit).toHaveBeenCalled())
    expect(onSubmit.mock.calls[0][0]).toMatchObject({ postalCode: "" })
  })

  it("shows the masked ID and never an input for the ID number when editing", () => {
    renderForm({ customer })

    expect(screen.getByText("NRIC *****567D")).toBeInTheDocument()
    expect(screen.queryByLabelText("ID number")).not.toBeInTheDocument()
  })

  it("sends company details, not personal names, for a business customer", async () => {
    const { onSubmit } = renderForm({ customer: business })

    expect(screen.queryByLabelText("First name")).not.toBeInTheDocument()
    fireEvent.click(screen.getByRole("button", { name: "Save Changes" }))

    await waitFor(() => expect(onSubmit).toHaveBeenCalled())
    expect(onSubmit.mock.calls[0][0]).toMatchObject({
      companyName: "Lion City Logistics Pte Ltd",
      contactName: "Priya Nair",
    })
    expect(onSubmit.mock.calls[0][0]).not.toHaveProperty("firstName")
  })

  it("collects identity details when a new individual is registered", async () => {
    const { onSubmit } = renderForm()

    fireEvent.change(screen.getByLabelText("First name"), { target: { value: "Mei Ling" } })
    fireEvent.change(screen.getByLabelText("Last name"), { target: { value: "Lim" } })
    fireEvent.change(screen.getByLabelText("ID number"), { target: { value: "s1234567d" } })
    fireEvent.change(screen.getByLabelText("Date of birth"), { target: { value: "1990-01-31" } })
    fireEvent.change(screen.getByLabelText("Email"), { target: { value: "meiling@example.com" } })
    fireEvent.change(screen.getByLabelText("Contact number"), { target: { value: "8123 4567" } })
    fireEvent.click(screen.getByRole("button", { name: "Add Customer" }))

    await waitFor(() => expect(onSubmit).toHaveBeenCalled())
    expect(onSubmit.mock.calls[0][0]).toEqual({
      type: "INDIVIDUAL",
      firstName: "Mei Ling",
      lastName: "Lim",
      idType: "NRIC",
      idNumber: "s1234567d",
      dateOfBirth: "1990-01-31",
      email: "meiling@example.com",
      phone: "8123 4567",
      addressLine: undefined,
      postalCode: undefined,
    })
  })
})
