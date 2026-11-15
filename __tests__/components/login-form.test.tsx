import { fireEvent, render, screen, waitFor } from "@testing-library/react"
import { describe, it, expect, vi } from "vitest"
import { LoginForm, type DemoAccounts } from "@/components/login-form"
import { ApiError } from "@/lib/api"

const demoAccounts: DemoAccounts = {
  password: "Demo-Pass-2026",
  accounts: [
    { username: "agent", display_name: "Priya Nair", role: "CARE_AGENT", role_label: "Care Agent" },
    {
      username: "product",
      display_name: "Rachel Wong",
      role: "PRODUCT_MANAGER",
      role_label: "Product Manager",
    },
  ],
}

describe("LoginForm", () => {
  it("signs in with the trimmed username and the password as typed", async () => {
    const onSignIn = vi.fn().mockResolvedValue(undefined)
    render(<LoginForm onSignIn={onSignIn} />)

    fireEvent.change(screen.getByLabelText("Username"), { target: { value: " agent " } })
    fireEvent.change(screen.getByLabelText("Password"), { target: { value: " secret pass " } })
    fireEvent.click(screen.getByRole("button", { name: "Sign in" }))

    await waitFor(() => expect(onSignIn).toHaveBeenCalledWith("agent", " secret pass "))
  })

  it("shows why signing in failed and lets the user try again", async () => {
    const onSignIn = vi
      .fn()
      .mockRejectedValue(new ApiError("The username or password is incorrect.", 401))
    render(<LoginForm onSignIn={onSignIn} />)

    fireEvent.change(screen.getByLabelText("Username"), { target: { value: "agent" } })
    fireEvent.change(screen.getByLabelText("Password"), { target: { value: "wrong" } })
    fireEvent.click(screen.getByRole("button", { name: "Sign in" }))

    expect(await screen.findByRole("alert")).toHaveTextContent(
      "The username or password is incorrect."
    )
    expect(screen.getByRole("button", { name: "Sign in" })).toBeEnabled()
  })

  it("signs in as a demo account with the shared demo password", async () => {
    const onSignIn = vi.fn().mockResolvedValue(undefined)
    render(<LoginForm onSignIn={onSignIn} demoAccounts={demoAccounts} />)

    expect(screen.getByText("Demo-Pass-2026")).toBeInTheDocument()
    fireEvent.click(screen.getByRole("button", { name: "Sign in as Rachel Wong, Product Manager" }))

    await waitFor(() => expect(onSignIn).toHaveBeenCalledWith("product", "Demo-Pass-2026"))
  })

  it("does not list demo accounts when the server shares no demo password", () => {
    render(
      <LoginForm onSignIn={vi.fn()} demoAccounts={{ password: null, accounts: [] }} notice={null} />
    )

    expect(screen.queryByText("Demo accounts")).not.toBeInTheDocument()
  })

  it("explains that the session has ended", () => {
    render(<LoginForm onSignIn={vi.fn()} notice="Your session has ended." />)

    expect(screen.getByRole("status")).toHaveTextContent("Your session has ended.")
  })
})
