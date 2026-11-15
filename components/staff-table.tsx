"use client"

import { KeyRound, LockOpen, Pencil } from "lucide-react"
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table"
import { Button } from "@/components/ui/button"
import { StatusBadge } from "@/components/status-badge"
import { formatDateTime } from "@/lib/format"
import type { StaffUser } from "@/hooks/use-staff-users"

interface StaffTableProps {
  users: StaffUser[]
  currentUserId: number | null
  onEdit: (user: StaffUser) => void
  onResetPassword: (user: StaffUser) => void
  onUnlock: (user: StaffUser) => void
}

function AccessBadge({ user }: { user: StaffUser }) {
  if (!user.is_active) return <StatusBadge value="INACTIVE" />
  if (user.is_locked) {
    return (
      <span title={`Locked until ${formatDateTime(user.locked_until)}`}>
        <StatusBadge value="LOCKED" />
      </span>
    )
  }
  return <StatusBadge value="ACTIVE" />
}

export function StaffTable({
  users,
  currentUserId,
  onEdit,
  onResetPassword,
  onUnlock,
}: StaffTableProps) {
  return (
    <Table>
      <TableHeader className="bg-background sticky top-0 z-10">
        <TableRow>
          <TableHead>Name</TableHead>
          <TableHead>Username</TableHead>
          <TableHead>Role</TableHead>
          <TableHead>Status</TableHead>
          <TableHead>Last sign-in</TableHead>
          <TableHead className="w-[130px] text-right">Actions</TableHead>
        </TableRow>
      </TableHeader>
      <TableBody>
        {users.map((user) => (
          <TableRow key={user.id}>
            <TableCell>
              <div className="font-medium">
                {user.display_name}
                {user.id === currentUserId && (
                  <span className="text-muted-foreground ml-1 text-xs">(you)</span>
                )}
              </div>
              <div className="text-muted-foreground text-xs">{user.email}</div>
            </TableCell>
            <TableCell className="font-mono text-sm">{user.username}</TableCell>
            <TableCell>{user.role_label}</TableCell>
            <TableCell>
              <AccessBadge user={user} />
            </TableCell>
            <TableCell className="text-muted-foreground whitespace-nowrap">
              {user.last_login_at ? formatDateTime(user.last_login_at) : "Never"}
            </TableCell>
            <TableCell className="text-right">
              <div className="flex justify-end gap-1">
                <Button
                  variant="ghost"
                  size="icon"
                  aria-label={`Edit ${user.username}`}
                  onClick={() => onEdit(user)}
                >
                  <Pencil className="h-4 w-4" />
                </Button>
                <Button
                  variant="ghost"
                  size="icon"
                  aria-label={`Reset password of ${user.username}`}
                  onClick={() => onResetPassword(user)}
                >
                  <KeyRound className="h-4 w-4" />
                </Button>
                {user.is_locked && (
                  <Button
                    variant="ghost"
                    size="icon"
                    aria-label={`Unlock ${user.username}`}
                    onClick={() => onUnlock(user)}
                  >
                    <LockOpen className="h-4 w-4" />
                  </Button>
                )}
              </div>
            </TableCell>
          </TableRow>
        ))}
      </TableBody>
    </Table>
  )
}
