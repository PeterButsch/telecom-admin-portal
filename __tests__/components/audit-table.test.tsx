import { render, screen } from "@testing-library/react"
import { describe, it, expect } from "vitest"
import { AuditTable } from "@/components/audit-table"
import type { AuditEvent } from "@/hooks/use-audit-events"

const base: AuditEvent = {
  id: 1,
  occurred_at: "2026-03-01T02:30:00Z",
  actor_id: 2,
  actor_username: "agent",
  actor_role: "CARE_AGENT",
  actor_role_label: "Care Agent",
  action: "CUSTOMER_UPDATED",
  entity_type: "CUSTOMER",
  entity_id: "42",
  summary: "Updated C1000042: email",
  outcome: "SUCCESS",
}

describe("AuditTable", () => {
  it("shows who did what to which record, in Singapore time", () => {
    render(<AuditTable events={[base]} />)

    expect(screen.getByText("1 Mar 2026, 10:30")).toBeInTheDocument()
    expect(screen.getByText("Care Agent")).toBeInTheDocument()
    expect(screen.getByText("Customer updated")).toBeInTheDocument()
    expect(screen.getByText("42")).toBeInTheDocument()
    expect(screen.getByText("Success")).toBeInTheDocument()
  })

  it("shows failed sign-ins by unknown usernames without a role or record number", () => {
    render(
      <AuditTable
        events={[
          {
            ...base,
            actor_id: null,
            actor_username: "nobody",
            actor_role: null,
            actor_role_label: null,
            action: "LOGIN_FAILED",
            entity_type: "SESSION",
            entity_id: "7",
            summary: "Sign-in failed: unknown username",
            outcome: "FAILED",
          },
        ]}
      />
    )

    expect(screen.getByText("nobody")).toBeInTheDocument()
    expect(screen.queryByText("-")).not.toBeInTheDocument()
    expect(screen.queryByText("7")).not.toBeInTheDocument()
    expect(screen.getByText("Failed")).toBeInTheDocument()
  })
})
