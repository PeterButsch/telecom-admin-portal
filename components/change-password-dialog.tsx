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
import { FormError } from "@/components/form-error"
import { TextField, describeSaveError, type FieldErrors } from "@/components/form-fields"
import { apiFetch } from "@/lib/api"

export const PASSWORD_HINT =
  "At least 12 characters with letters and digits, and not containing the username."

function ChangePasswordForm({ onClose }: { onClose: () => void }) {
  const [currentPassword, setCurrentPassword] = useState("")
  const [newPassword, setNewPassword] = useState("")
  const [confirmPassword, setConfirmPassword] = useState("")
  const [submitting, setSubmitting] = useState(false)
  const [done, setDone] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [fieldErrors, setFieldErrors] = useState<FieldErrors>({})

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setError(null)
    setFieldErrors({})
    if (newPassword !== confirmPassword) {
      setFieldErrors({ confirmPassword: "does not match the new password" })
      return
    }
    setSubmitting(true)
    try {
      await apiFetch("/api/auth/password", {
        method: "POST",
        body: { currentPassword, newPassword },
      })
      setDone(true)
    } catch (err) {
      const { form, fields } = describeSaveError(err)
      setError(form)
      setFieldErrors(fields)
    } finally {
      setSubmitting(false)
    }
  }

  if (done) {
    return (
      <>
        <DialogHeader>
          <DialogTitle>Password changed</DialogTitle>
          <DialogDescription>
            Your new password is active. Any other sessions you had open have been signed out.
          </DialogDescription>
        </DialogHeader>
        <DialogFooter>
          <Button onClick={onClose}>Close</Button>
        </DialogFooter>
      </>
    )
  }

  return (
    <form onSubmit={handleSubmit}>
      <DialogHeader>
        <DialogTitle>Change password</DialogTitle>
        <DialogDescription>{PASSWORD_HINT}</DialogDescription>
      </DialogHeader>
      <div className="grid gap-4 py-4">
        <TextField
          id="currentPassword"
          label="Current password"
          type="password"
          autoComplete="current-password"
          value={currentPassword}
          onChange={setCurrentPassword}
          error={fieldErrors.currentPassword}
          required
        />
        <TextField
          id="newPassword"
          label="New password"
          type="password"
          autoComplete="new-password"
          value={newPassword}
          onChange={setNewPassword}
          error={fieldErrors.newPassword}
          required
        />
        <TextField
          id="confirmPassword"
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
          {submitting ? "Saving..." : "Change password"}
        </Button>
      </DialogFooter>
    </form>
  )
}

export function ChangePasswordDialog({
  open,
  onOpenChange,
}: {
  open: boolean
  onOpenChange: (open: boolean) => void
}) {
  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-[440px]">
        {open && <ChangePasswordForm onClose={() => onOpenChange(false)} />}
      </DialogContent>
    </Dialog>
  )
}
