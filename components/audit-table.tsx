"use client"

import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table"
import { StatusBadge } from "@/components/status-badge"
import { formatDateTime } from "@/lib/format"
import { label } from "@/lib/labels"
import type { AuditEvent } from "@/hooks/use-audit-events"

export function AuditTable({ events }: { events: AuditEvent[] }) {
  return (
    <Table>
      <TableHeader className="bg-background sticky top-0 z-10">
        <TableRow>
          <TableHead>Time</TableHead>
          <TableHead>Staff</TableHead>
          <TableHead>Action</TableHead>
          <TableHead>Record</TableHead>
          <TableHead>Details</TableHead>
          <TableHead>Outcome</TableHead>
        </TableRow>
      </TableHeader>
      <TableBody>
        {events.map((event) => (
          <TableRow key={event.id}>
            <TableCell className="text-muted-foreground whitespace-nowrap">
              {formatDateTime(event.occurred_at)}
            </TableCell>
            <TableCell>
              {event.actor_username ? (
                <>
                  <div className="font-mono text-sm">{event.actor_username}</div>
                  {event.actor_role && (
                    <div className="text-muted-foreground text-xs">
                      {event.actor_role_label ?? label(event.actor_role)}
                    </div>
                  )}
                </>
              ) : (
                <span className="text-muted-foreground">Not signed in</span>
              )}
            </TableCell>
            <TableCell className="whitespace-nowrap">{label(event.action)}</TableCell>
            <TableCell className="whitespace-nowrap">
              {label(event.entity_type)}
              {event.entity_id && event.entity_type !== "SESSION" && (
                <span className="text-muted-foreground ml-1 font-mono text-xs">
                  {event.entity_id}
                </span>
              )}
            </TableCell>
            <TableCell className="max-w-[420px]">
              <div className="break-words">{event.summary}</div>
            </TableCell>
            <TableCell>
              <StatusBadge value={event.outcome} />
            </TableCell>
          </TableRow>
        ))}
      </TableBody>
    </Table>
  )
}
