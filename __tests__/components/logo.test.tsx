import { render, screen } from "@testing-library/react"
import { describe, it, expect } from "vitest"
import { Logo, Wordmark } from "@/components/logo"

describe("Logo", () => {
  it("renders the Singtel mark", () => {
    render(<Logo />)
    expect(screen.getByRole("img", { name: /singtel logo/i })).toBeInTheDocument()
  })

  it("applies custom className", () => {
    render(<Logo className="custom-class" />)
    expect(screen.getByRole("img", { name: /singtel logo/i })).toHaveClass("custom-class")
  })
})

describe("Wordmark", () => {
  it("shows the brand name and the product", () => {
    render(<Wordmark product="Ops Console" />)
    expect(screen.getByText("Singtel")).toHaveClass("text-brand")
    expect(screen.getByText("Ops Console")).toBeInTheDocument()
  })
})
