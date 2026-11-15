"use client"

import { useEffect, useState } from "react"
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog"
import { Button } from "@/components/ui/button"
import { Label } from "@/components/ui/label"
import { Textarea } from "@/components/ui/textarea"
import { CustomerPicker } from "@/components/customer-picker"
import { FormError } from "@/components/form-error"
import {
  ReadOnlyField,
  SelectField,
  TextField,
  describeSaveError,
  type FieldErrors,
} from "@/components/form-fields"
import { apiFetch } from "@/lib/api"
import { formatMsisdn } from "@/lib/format"
import { options } from "@/lib/labels"
import type { Customer } from "@/hooks/use-customers"
import type { Line } from "@/hooks/use-lines"
import type { PagedResponse } from "@/hooks/use-paged-list"
import {
  CASE_CATEGORIES,
  CASE_CHANNELS,
  CASE_PRIORITIES,
  CASE_STATUSES,
  type CaseCategory,
  type CaseChannel,
  type CaseCreate,
  type CasePriority,
  type CaseStatus,
  type CaseUpdate,
  type SupportCase,
} from "@/hooks/use-cases"

interface CaseFormDialogProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  supportCase: SupportCase | null
  onSubmit: (data: CaseCreate | CaseUpdate) => Promise<void>
}

const NO_LINE = "none"

/** Lines of the chosen customer, so a case can be raised against one of them. */
function useCustomerLines(customerId: number | null) {
  const [lines, setLines] = useState<Line[]>([])
  useEffect(() => {
    if (!customerId) return
    const controller = new AbortController()
    apiFetch<PagedResponse<Line>>(`/api/lines?customerId=${customerId}&size=100`, {
      signal: controller.signal,
    })
      .then((page) => setLines(page.content))
      .catch(() => {
        if (!controller.signal.aborted) setLines([])
      })
    return () => controller.abort()
  }, [customerId])
  return customerId ? lines : []
}

function CaseForm({
  supportCase,
  onSubmit,
  onCancel,
}: {
  supportCase: SupportCase | null
  onSubmit: (data: CaseCreate | CaseUpdate) => Promise<void>
  onCancel: () => void
}) {
  const isEditing = supportCase !== null
  const [customer, setCustomer] = useState<Customer | null>(null)
  const [msisdn, setMsisdn] = useState(NO_LINE)
  const [subject, setSubject] = useState(supportCase?.subject ?? "")
  const [description, setDescription] = useState(supportCase?.description ?? "")
  const [category, setCategory] = useState<CaseCategory>(supportCase?.category ?? "GENERAL")
  const [channel, setChannel] = useState<CaseChannel>("PHONE")
  const [priority, setPriority] = useState<CasePriority>(supportCase?.priority ?? "MEDIUM")
  const [status, setStatus] = useState<CaseStatus>(supportCase?.status ?? "OPEN")
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [fieldErrors, setFieldErrors] = useState<FieldErrors>({})
  const lines = useCustomerLines(customer?.id ?? null)

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setError(null)
    setFieldErrors({})
    if (!isEditing && !customer) {
      setFieldErrors({ customerId: "Choose the customer this case is for." })
      return
    }
    setSubmitting(true)
    try {
      if (isEditing) {
        await onSubmit({ subject: subject.trim(), description, category, priority, status })
      } else if (customer) {
        await onSubmit({
          customerId: customer.id,
          msisdn: msisdn === NO_LINE ? undefined : msisdn,
          category,
          channel,
          subject: subject.trim(),
          description: description.trim() || undefined,
          priority,
        })
      }
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
        <DialogTitle>{isEditing ? `Case ${supportCase.case_number}` : "New Case"}</DialogTitle>
        <DialogDescription>
          {isEditing
            ? "Update the case. Closed cases can no longer be changed."
            : "Log a customer contact. The SLA target follows from the priority."}
        </DialogDescription>
      </DialogHeader>
      <div className="grid gap-4 py-4">
        {isEditing ? (
          <div className="grid grid-cols-2 gap-4">
            <ReadOnlyField
              label="Customer"
              value={`${supportCase.customer_name} (${supportCase.customer_number})`}
            />
            <ReadOnlyField
              label="Mobile number"
              value={supportCase.msisdn ? formatMsisdn(supportCase.msisdn) : "Not line-specific"}
            />
          </div>
        ) : (
          <>
            <CustomerPicker
              id="customer"
              value={customer}
              onChange={(c) => {
                setCustomer(c)
                setMsisdn(NO_LINE)
              }}
              error={fieldErrors.customerId}
            />
            {customer && (
              <SelectField
                id="msisdn"
                label="Mobile number"
                value={msisdn}
                options={[
                  { value: NO_LINE, label: "Not line-specific" },
                  ...lines.map((line) => ({
                    value: line.msisdn,
                    label: `${formatMsisdn(line.msisdn)} · ${line.plan_name}`,
                  })),
                ]}
                onChange={setMsisdn}
                error={fieldErrors.msisdn}
              />
            )}
          </>
        )}
        <TextField
          id="subject"
          label="Subject"
          value={subject}
          onChange={setSubject}
          error={fieldErrors.subject}
          placeholder="Short summary of the issue"
          required
        />
        <div className="space-y-2">
          <Label htmlFor="description">Details</Label>
          <Textarea
            id="description"
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            placeholder="What the customer reported and what you have checked"
            rows={4}
          />
        </div>
        <div className="grid grid-cols-3 gap-4">
          <SelectField
            id="category"
            label="Category"
            value={category}
            options={options(CASE_CATEGORIES)}
            onChange={(v) => setCategory(v as CaseCategory)}
            error={fieldErrors.category}
          />
          {isEditing ? (
            <SelectField
              id="status"
              label="Status"
              value={status}
              options={options(CASE_STATUSES)}
              onChange={(v) => setStatus(v as CaseStatus)}
              error={fieldErrors.status}
            />
          ) : (
            <SelectField
              id="channel"
              label="Channel"
              value={channel}
              options={options(CASE_CHANNELS)}
              onChange={(v) => setChannel(v as CaseChannel)}
              error={fieldErrors.channel}
            />
          )}
          <SelectField
            id="priority"
            label="Priority"
            value={priority}
            options={options(CASE_PRIORITIES)}
            onChange={(v) => setPriority(v as CasePriority)}
            error={fieldErrors.priority}
          />
        </div>
      </div>
      <FormError message={error} />
      <DialogFooter>
        <Button type="button" variant="outline" onClick={onCancel}>
          Cancel
        </Button>
        <Button type="submit" disabled={submitting}>
          {submitting ? "Saving..." : isEditing ? "Save Changes" : "Create Case"}
        </Button>
      </DialogFooter>
    </form>
  )
}

export function CaseFormDialog({ open, onOpenChange, supportCase, onSubmit }: CaseFormDialogProps) {
  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-[560px]">
        {open && (
          <CaseForm
            key={supportCase?.id ?? "new"}
            supportCase={supportCase}
            onSubmit={onSubmit}
            onCancel={() => onOpenChange(false)}
          />
        )}
      </DialogContent>
    </Dialog>
  )
}
