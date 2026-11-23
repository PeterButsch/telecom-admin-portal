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
import {
  ReadOnlyField,
  SelectField,
  TextField,
  describeSaveError,
  type FieldErrors,
} from "@/components/form-fields"
import { options } from "@/lib/labels"
import {
  BRANDS,
  NETWORKS,
  PLAN_CATEGORIES,
  SEGMENTS,
  type Brand,
  type Plan,
  type PlanCategory,
  type PlanRequest,
  type Segment,
} from "@/hooks/use-plans"

interface PlanFormDialogProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  plan: Plan | null
  onSubmit: (data: PlanRequest) => Promise<void>
}

const limit = (value: string) => (value.trim() === "" ? null : Number(value))

function PlanForm({
  plan,
  onSubmit,
  onCancel,
}: {
  plan: Plan | null
  onSubmit: (data: PlanRequest) => Promise<void>
  onCancel: () => void
}) {
  const isEditing = plan !== null
  const [code, setCode] = useState(plan?.code ?? "")
  const [name, setName] = useState(plan?.name ?? "")
  const [brand, setBrand] = useState<Brand>(plan?.brand ?? "SINGTEL")
  const [category, setCategory] = useState<PlanCategory>(plan?.category ?? "POSTPAID")
  const [segment, setSegment] = useState<Segment>(plan?.segment ?? "CONSUMER")
  const [monthlyPrice, setMonthlyPrice] = useState(plan?.monthly_price?.toString() ?? "")
  const [dataLimitGb, setDataLimitGb] = useState(plan?.data_limit_gb?.toString() ?? "")
  const [minutesLimit, setMinutesLimit] = useState(plan?.minutes_limit?.toString() ?? "")
  const [smsLimit, setSmsLimit] = useState(plan?.sms_limit?.toString() ?? "")
  const [contractMonths, setContractMonths] = useState(plan?.contract_months?.toString() ?? "0")
  const [network, setNetwork] = useState(plan?.network ?? "5G")
  const [isActive, setIsActive] = useState(plan?.is_active ?? true)
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [fieldErrors, setFieldErrors] = useState<FieldErrors>({})

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setSubmitting(true)
    setError(null)
    setFieldErrors({})
    try {
      await onSubmit({
        code: code.trim(),
        name: name.trim(),
        brand,
        category,
        segment,
        monthlyPrice: Number(monthlyPrice),
        dataLimitGb: limit(dataLimitGb),
        minutesLimit: limit(minutesLimit),
        smsLimit: limit(smsLimit),
        contractMonths: Number(contractMonths) || 0,
        network,
        isActive,
      })
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
        <DialogTitle>{isEditing ? "Edit Plan" : "Add Plan"}</DialogTitle>
        <DialogDescription>
          {isEditing
            ? "Update the plan details below. The plan code cannot be changed."
            : "Add a plan to the Singtel, GOMO or hi!Card catalogue."}
        </DialogDescription>
      </DialogHeader>
      <div className="grid gap-4 py-4">
        <div className="grid grid-cols-3 gap-4">
          {isEditing ? (
            <ReadOnlyField label="Code" value={<span className="font-mono">{code}</span>} />
          ) : (
            <TextField
              id="code"
              label="Code"
              value={code}
              onChange={(v) => setCode(v.toUpperCase())}
              error={fieldErrors.code}
              placeholder="XO-5G-150"
              required
            />
          )}
          <TextField
            id="name"
            label="Plan name"
            className="col-span-2"
            value={name}
            onChange={setName}
            error={fieldErrors.name}
            placeholder="e.g. XO Plus 150GB"
            required
          />
        </div>
        <div className="grid grid-cols-3 gap-4">
          <SelectField
            id="brand"
            label="Brand"
            value={brand}
            options={options(BRANDS)}
            onChange={(v) => setBrand(v as Brand)}
            error={fieldErrors.brand}
          />
          <SelectField
            id="category"
            label="Category"
            value={category}
            options={options(PLAN_CATEGORIES)}
            onChange={(v) => setCategory(v as PlanCategory)}
            error={fieldErrors.category}
          />
          <SelectField
            id="segment"
            label="Segment"
            value={segment}
            options={options(SEGMENTS)}
            onChange={(v) => setSegment(v as Segment)}
            error={fieldErrors.segment}
          />
        </div>
        <div className="grid grid-cols-3 gap-4">
          <TextField
            id="monthlyPrice"
            label="Price per month (S$)"
            type="number"
            min="0"
            step="0.01"
            value={monthlyPrice}
            onChange={setMonthlyPrice}
            error={fieldErrors.monthlyPrice}
            placeholder="0.00"
            required
          />
          <TextField
            id="contractMonths"
            label="Contract (months)"
            type="number"
            min="0"
            value={contractMonths}
            onChange={setContractMonths}
            error={fieldErrors.contractMonths}
          />
          <SelectField
            id="network"
            label="Network"
            value={network}
            options={NETWORKS.map((n) => ({ value: n, label: n }))}
            onChange={setNetwork}
            error={fieldErrors.network}
          />
        </div>
        <div className="grid grid-cols-3 gap-4">
          <TextField
            id="dataLimitGb"
            label="Data (GB)"
            type="number"
            min="0"
            value={dataLimitGb}
            onChange={setDataLimitGb}
            error={fieldErrors.dataLimitGb}
            placeholder="Unlimited"
          />
          <TextField
            id="minutesLimit"
            label="Talktime (min)"
            type="number"
            min="0"
            value={minutesLimit}
            onChange={setMinutesLimit}
            error={fieldErrors.minutesLimit}
            placeholder="Unlimited"
          />
          <TextField
            id="smsLimit"
            label="SMS"
            type="number"
            min="0"
            value={smsLimit}
            onChange={setSmsLimit}
            error={fieldErrors.smsLimit}
            placeholder="Unlimited"
          />
        </div>
        {isEditing && (
          <SelectField
            id="isActive"
            label="Availability"
            value={isActive ? "active" : "inactive"}
            options={[
              { value: "active", label: "Active: can be sold" },
              { value: "inactive", label: "Inactive: no new sign-ups" },
            ]}
            onChange={(v) => setIsActive(v === "active")}
          />
        )}
      </div>
      <FormError message={error} />
      <DialogFooter>
        <Button type="button" variant="outline" onClick={onCancel}>
          Cancel
        </Button>
        <Button type="submit" disabled={submitting}>
          {submitting ? "Saving..." : isEditing ? "Save Changes" : "Add Plan"}
        </Button>
      </DialogFooter>
    </form>
  )
}

export function PlanFormDialog({ open, onOpenChange, plan, onSubmit }: PlanFormDialogProps) {
  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-[560px]">
        {open && (
          <PlanForm
            key={plan?.id ?? "new"}
            plan={plan}
            onSubmit={onSubmit}
            onCancel={() => onOpenChange(false)}
          />
        )}
      </DialogContent>
    </Dialog>
  )
}
