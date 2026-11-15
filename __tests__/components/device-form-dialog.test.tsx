import { fireEvent, render, screen, waitFor } from "@testing-library/react"
import { describe, it, expect, vi } from "vitest"
import { DeviceFormDialog } from "@/components/device-form-dialog"
import type { Device } from "@/hooks/use-devices"

const assigned: Device = {
  id: 7,
  imei: "356938035643809",
  manufacturer: "Apple",
  model: "iPhone 16 Pro",
  status: "ASSIGNED",
  line_id: 3,
  msisdn: "91234567",
  customer_id: 1,
  customer_name: "Tan Wei Ming",
  assigned_at: "2026-01-01T00:00:00Z",
  created_at: "2026-01-01T00:00:00Z",
}

function renderForm(device: Device | null) {
  const onSubmit = vi.fn().mockResolvedValue(undefined)
  render(<DeviceFormDialog open onOpenChange={vi.fn()} device={device} onSubmit={onSubmit} />)
  return onSubmit
}

describe("DeviceFormDialog", () => {
  it("keeps an assigned device on its line when saved unchanged", async () => {
    const onSubmit = renderForm(assigned)

    expect(screen.getByLabelText("Mobile number")).toHaveValue("9123 4567")
    fireEvent.click(screen.getByRole("button", { name: "Save Changes" }))

    await waitFor(() => expect(onSubmit).toHaveBeenCalled())
    expect(onSubmit.mock.calls[0][0]).toEqual({
      imei: "356938035643809",
      manufacturer: "Apple",
      model: "iPhone 16 Pro",
      status: "ASSIGNED",
      msisdn: "9123 4567",
    })
  })

  it("registers a new device in stock when no number is given", async () => {
    const onSubmit = renderForm(null)

    fireEvent.change(screen.getByLabelText("IMEI"), { target: { value: "356938035643810" } })
    fireEvent.change(screen.getByLabelText("Manufacturer"), { target: { value: "Samsung" } })
    fireEvent.change(screen.getByLabelText("Model"), { target: { value: "Galaxy S25" } })
    fireEvent.click(screen.getByRole("button", { name: "Add Device" }))

    await waitFor(() => expect(onSubmit).toHaveBeenCalled())
    expect(onSubmit.mock.calls[0][0]).toEqual({
      imei: "356938035643810",
      manufacturer: "Samsung",
      model: "Galaxy S25",
      msisdn: undefined,
    })
  })
})
