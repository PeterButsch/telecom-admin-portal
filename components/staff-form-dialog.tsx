"use client"

import { useState } from "react"
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog"
import { Button } from "@/components/ui/button"
import { PASSWORD_HINT } from "@/components/change-password-dialog"
import { FormError } from "@/components/form-error"
import {
  ReadOnlyField,
  SelectField,
  TextField,
  describeSaveError,
  type FieldErrors,
} from "@/components/form-fields"
import { options } from "@/lib/labels"
import { STAFF_ROLES, type StaffRole } from "@/lib/permissions"
import type { StaffUser, StaffUserCreate, StaffUserUpdate } from "@/hooks/use-staff-users"

interface StaffFormDialogProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  user: StaffUser | null
  /** The signed-in administrator, who cannot change their own role or deactivate themselves. */
  currentUserId: number | null
  onSubmit: (data: StaffUserCreate | StaffUserUpdate) => Promise<void>
}

function StaffForm({
  user,
  currentUserId,
  onSubmit,
  onCancel,
}: {
  user: StaffUser | null
  currentUserId: number | null
  onSubmit: (data: StaffUserCreate | StaffUserUpdate) => Promise<void>
  onCancel: () => void
}) {
  const isEditing = user !== null
  const isSelf = isEditing && user.id === currentUserId
  const [username, setUsername] = useState("")
  const [displayName, setDisplayName] = useState(user?.display_name ?? "")
  const [email, setEmail] = useState(user?.email ?? "")
  const [role, setRole] = useState<StaffRole>(user?.role ?? "CARE_AGENT")
  const [active, setActive] = useState(user?.is_active ?? true)
  const [password, setPassword] = useState("")
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [fieldErrors, setFieldErrors] = useState<FieldErrors>({})

  const buildRequest = (): StaffUserCreate | StaffUserUpdate => {
    if (!isEditing) {
      return {
        username: username.trim().toLowerCase(),
        displayName: displayName.trim(),
        email: email.trim(),
        role,
        password,
      }
    }
    const changes: StaffUserUpdate = {}
    if (displayName.trim() !== user.display_name) changes.displayName = displayName.trim()
    if (email.trim() !== user.email) changes.email = email.trim()
    if (role !== user.role) changes.role = role
    if (active !== user.is_active) changes.active = active
    return changes
  }

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setSubmitting(true)
    setError(null)
    setFieldErrors({})
    try {
      await onSubmit(buildRequest())
      onCancel()
    } catch (err) {
      const { form, fields } = describeSaveError(err)
      setError(form)
      setFieldErrors(fields)
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <form onSubmit={handleSubmit}>
      <DialogHeader>
        <DialogTitle>{isEditing ? "Edit Staff Account" : "Add Staff Account"}</DialogTitle>
        <DialogDescription>
          {isEditing
            ? "Role changes take effect on the person's next request."
            : "The new staff member signs in with this username and password."}
        </DialogDescription>
      </DialogHeader>
      <div className="grid gap-4 py-4">
        <div className="grid grid-cols-2 gap-4">
          {isEditing ? (
            <ReadOnlyField
              label="Username"
              value={<span className="font-mono">{user.username}</span>}
            />
          ) : (
            <TextField
              id="staff-username"
              label="Username"
              value={username}
              onChange={setUsername}
              error={fieldErrors.username}
              autoComplete="off"
              autoCapitalize="none"
              spellCheck={false}
              placeholder="e.g. jtan"
              required
            />
          )}
          <TextField
            id="staff-name"
            label="Full name"
            value={displayName}
            onChange={setDisplayName}
            error={fieldErrors.displayName}
            required
          />
        </div>
        <TextField
          id="staff-email"
          label="Work email"
          type="email"
          value={email}
          onChange={setEmail}
          error={fieldErrors.email}
          required
        />
        <div className="grid grid-cols-2 gap-4">
          <SelectField
            id="staff-role"
            label="Role"
            value={role}
            options={options(STAFF_ROLES)}
            onChange={(v) => setRole(v as StaffRole)}
            error={fieldErrors.role}
            disabled={isSelf}
          />
          {isEditing && (
            <SelectField
              id="staff-active"
              label="Access"
              value={active ? "active" : "inactive"}
              options={[
                { value: "active", label: "Active: can sign in" },
                { value: "inactive", label: "Inactive: cannot sign in" },
              ]}
              onChange={(v) => setActive(v === "active")}
              error={fieldErrors.active}
              disabled={isSelf}
            />
          )}
        </div>
        {isSelf && (
          <p className="text-muted-foreground text-xs">
            You cannot change your own role or deactivate your own account.
          </p>
        )}
        {!isEditing && (
          <TextField
            id="staff-password"
            label="Initial password"
            type="password"
            autoComplete="new-password"
            value={password}
            onChange={setPassword}
            error={fieldErrors.password}
            hint={PASSWORD_HINT}
            required
          />
        )}
      </div>
      <FormError message={error} />
      <DialogFooter>
        <Button type="button" variant="outline" onClick={onCancel}>
          Cancel
        </Button>
        <Button type="submit" disabled={submitting}>
          {submitting ? "Saving..." : isEditing ? "Save Changes" : "Add Staff"}
        </Button>
      </DialogFooter>
    </form>
  )
}

export function StaffFormDialog({
  open,
  onOpenChange,
  user,
  currentUserId,
  onSubmit,
}: StaffFormDialogProps) {
  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-[520px]">
        {open && (
          <StaffForm
            key={user?.id ?? "new"}
            user={user}
            currentUserId={currentUserId}
            onSubmit={onSubmit}
            onCancel={() => onOpenChange(false)}
          />
        )}
      </DialogContent>
    </Dialog>
  )
}
