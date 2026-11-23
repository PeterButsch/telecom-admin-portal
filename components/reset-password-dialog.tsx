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
import { TextField, describeSaveError, type FieldErrors } from "@/components/form-fields"
import type { StaffUser } from "@/hooks/use-staff-users"

function ResetPasswordForm({
  user,
  onSubmit,
  onClose,
}: {
  user: StaffUser
  onSubmit: (password: string) => Promise<void>
  onClose: () => void
}) {
  const [password, setPassword] = useState("")
  const [confirmPassword, setConfirmPassword] = useState("")
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [fieldErrors, setFieldErrors] = useState<FieldErrors>({})

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setError(null)
    setFieldErrors({})
    if (password !== confirmPassword) {
      setFieldErrors({ confirmPassword: "does not match the new password" })
      return
    }
    setSubmitting(true)
    try {
      await onSubmit(password)
      onClose()
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
        <DialogTitle>Reset password</DialogTitle>
        <DialogDescription>
          Set a new password for {user.display_name} ({user.username}). Their open sessions end and
          any lock is cleared. Share the password with them through a secure channel.
        </DialogDescription>
      </DialogHeader>
      <div className="grid gap-4 py-4">
        <TextField
          id="reset-password"
          label="New password"
          type="password"
          autoComplete="new-password"
          value={password}
          onChange={setPassword}
          error={fieldErrors.password}
          hint={PASSWORD_HINT}
          required
        />
        <TextField
          id="reset-confirm"
          label="Confirm new password"
          type="password"
          autoComplete="new-password"
          value={confirmPassword}
          onChange={setConfirmPassword}
          error={fieldErrors.confirmPassword}
          required
        />
      </div>
      <FormError message={error} />
      <DialogFooter>
        <Button type="button" variant="outline" onClick={onClose}>
          Cancel
        </Button>
        <Button type="submit" disabled={submitting}>
          {submitting ? "Saving..." : "Reset password"}
        </Button>
      </DialogFooter>
    </form>
  )
}

export function ResetPasswordDialog({
  user,
  onOpenChange,
  onSubmit,
}: {
  user: StaffUser | null
  onOpenChange: (open: boolean) => void
  onSubmit: (password: string) => Promise<void>
}) {
  return (
    <Dialog open={user !== null} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-[460px]">
        {user && (
          <ResetPasswordForm
            key={user.id}
            user={user}
            onSubmit={onSubmit}
            onClose={() => onOpenChange(false)}
          />
        )}
      </DialogContent>
    </Dialog>
  )
}
