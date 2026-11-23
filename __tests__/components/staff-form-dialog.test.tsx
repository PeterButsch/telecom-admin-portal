import { fireEvent, render, screen, waitFor } from "@testing-library/react"
import { describe, it, expect, vi } from "vitest"
import { StaffFormDialog } from "@/components/staff-form-dialog"
import { ApiError } from "@/lib/api"
import type { StaffUser } from "@/hooks/use-staff-users"

const agent: StaffUser = {
  id: 2,
  username: "agent",
  display_name: "Priya Nair",
  email: "priya.nair@example.com",
  role: "CARE_AGENT",
  role_label: "Care Agent",
  is_active: true,
  is_locked: false,
  locked_until: null,
  failed_logins: 0,
  last_login_at: null,
  created_at: null,
}

function renderForm(props: Partial<React.ComponentProps<typeof StaffFormDialog>> = {}) {
  const onSubmit = vi.fn().mockResolvedValue(undefined)
  const onOpenChange = vi.fn()
  render(
    <StaffFormDialog
      open
      onOpenChange={onOpenChange}
      user={null}
      currentUserId={1}
      onSubmit={onSubmit}
      {...props}
    />
  )
  return { onSubmit: (props.onSubmit as typeof onSubmit) ?? onSubmit, onOpenChange }
}

describe("StaffFormDialog", () => {
  it("creates an account with a lower-case username", async () => {
    const { onSubmit, onOpenChange } = renderForm()

    fireEvent.change(screen.getByLabelText("Username"), { target: { value: " JTan " } })
    fireEvent.change(screen.getByLabelText("Full name"), { target: { value: "Jun Tan" } })
    fireEvent.change(screen.getByLabelText("Work email"), {
      target: { value: "jun.tan@example.com" },
    })
    fireEvent.change(screen.getByLabelText("Initial password"), {
      target: { value: "Welcome-2026-abc" },
    })
    fireEvent.click(screen.getByRole("button", { name: "Add Staff" }))

    await waitFor(() => expect(onOpenChange).toHaveBeenCalledWith(false))
    expect(onSubmit).toHaveBeenCalledWith({
      username: "jtan",
      displayName: "Jun Tan",
      email: "jun.tan@example.com",
      role: "CARE_AGENT",
      password: "Welcome-2026-abc",
    })
  })

  it("sends only the fields that changed", async () => {
    const { onSubmit } = renderForm({ user: agent })

    expect(screen.queryByLabelText("Initial password")).not.toBeInTheDocument()
    fireEvent.change(screen.getByLabelText("Work email"), {
      target: { value: "priya@example.com" },
    })
    fireEvent.click(screen.getByRole("button", { name: "Save Changes" }))

    await waitFor(() => expect(onSubmit).toHaveBeenCalledWith({ email: "priya@example.com" }))
  })

  it("does not let administrators change their own role or access", () => {
    renderForm({ user: { ...agent, id: 1, role: "ADMIN", role_label: "Administrator" } })

    expect(screen.getByLabelText("Role")).toBeDisabled()
    expect(screen.getByLabelText("Access")).toBeDisabled()
    expect(screen.getByText(/cannot change your own role/)).toBeInTheDocument()
  })

  it("shows the password policy message next to the password", async () => {
    const onSubmit = vi.fn().mockRejectedValue(
      new ApiError("password must be at least 12 characters", 400, {
        password: "must be at least 12 characters",
      })
    )
    renderForm({ onSubmit })

    fireEvent.change(screen.getByLabelText("Username"), { target: { value: "jtan" } })
    fireEvent.change(screen.getByLabelText("Full name"), { target: { value: "Jun Tan" } })
    fireEvent.change(screen.getByLabelText("Work email"), {
      target: { value: "jun.tan@example.com" },
    })
    fireEvent.change(screen.getByLabelText("Initial password"), { target: { value: "short" } })
    fireEvent.click(screen.getByRole("button", { name: "Add Staff" }))

    expect(await screen.findByText("must be at least 12 characters")).toBeInTheDocument()
    expect(screen.getByLabelText("Initial password")).toHaveAttribute("aria-invalid", "true")
  })
})
