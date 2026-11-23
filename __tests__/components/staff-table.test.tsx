import { render, screen } from "@testing-library/react"
import { describe, it, expect, vi } from "vitest"
import { StaffTable } from "@/components/staff-table"
import type { StaffUser } from "@/hooks/use-staff-users"

function user(id: number, username: string): StaffUser {
  return {
    id,
    username,
    display_name: username,
    email: `${username}@example.com`,
    role: "CARE_AGENT",
    role_label: "Care Agent",
    is_active: true,
    is_locked: false,
    locked_until: null,
    failed_logins: 0,
    last_login_at: null,
    created_at: null,
  }
}

describe("StaffTable", () => {
  it("offers a password reset for other staff but not for the signed-in administrator", () => {
    render(
      <StaffTable
        users={[user(1, "admin"), user(2, "agent")]}
        currentUserId={1}
        onEdit={vi.fn()}
        onResetPassword={vi.fn()}
        onUnlock={vi.fn()}
      />
    )

    expect(screen.getByRole("button", { name: "Reset password of agent" })).toBeInTheDocument()
    expect(screen.queryByRole("button", { name: "Reset password of admin" })).toBeNull()
    expect(screen.getByRole("button", { name: "Edit admin" })).toBeInTheDocument()
  })
})
