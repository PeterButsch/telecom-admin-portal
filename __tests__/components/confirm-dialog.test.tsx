import { fireEvent, render, screen, waitFor } from "@testing-library/react"
import { describe, it, expect, vi } from "vitest"
import { ConfirmDialog } from "@/components/confirm-dialog"
import { ApiError } from "@/lib/api"

describe("ConfirmDialog", () => {
  it("shows why the action failed and stays open", async () => {
    const onConfirm = vi
      .fn()
      .mockRejectedValue(new ApiError("Plan 4 is used by 3 customer(s).", 409))
    const onOpenChange = vi.fn()
    render(
      <ConfirmDialog
        open
        onOpenChange={onOpenChange}
        title="Delete Plan"
        description="Delete the plan?"
        onConfirm={onConfirm}
      />
    )

    fireEvent.click(screen.getByRole("button", { name: "Delete" }))

    expect(await screen.findByRole("alert")).toHaveTextContent("Plan 4 is used by 3 customer(s).")
    expect(onOpenChange).not.toHaveBeenCalledWith(false)
  })

  it("closes once the action succeeds", async () => {
    const onOpenChange = vi.fn()
    render(
      <ConfirmDialog
        open
        onOpenChange={onOpenChange}
        title="Delete Plan"
        description="Delete the plan?"
        onConfirm={vi.fn().mockResolvedValue(undefined)}
      />
    )

    fireEvent.click(screen.getByRole("button", { name: "Delete" }))

    await waitFor(() => expect(onOpenChange).toHaveBeenCalledWith(false))
  })
})
