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
  SelectField,
  TextField,
  describeSaveError,
  type FieldErrors,
} from "@/components/form-fields"
import { formatMsisdn } from "@/lib/format"
import { options } from "@/lib/labels"
import {
  DEVICE_STATUSES,
  type Device,
  type DeviceCreate,
  type DeviceStatus,
  type DeviceUpdate,
} from "@/hooks/use-devices"

interface DeviceFormDialogProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  device: Device | null
  onSubmit: (data: DeviceCreate | DeviceUpdate) => Promise<void>
}

function DeviceForm({
  device,
  onSubmit,
  onCancel,
}: {
  device: Device | null
  onSubmit: (data: DeviceCreate | DeviceUpdate) => Promise<void>
  onCancel: () => void
}) {
  const isEditing = device !== null
  const [imei, setImei] = useState(device?.imei ?? "")
  const [manufacturer, setManufacturer] = useState(device?.manufacturer ?? "")
  const [model, setModel] = useState(device?.model ?? "")
  const [msisdn, setMsisdn] = useState(device?.msisdn ? formatMsisdn(device.msisdn) : "")
  const [status, setStatus] = useState<DeviceStatus>(device?.status ?? "AVAILABLE")
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [fieldErrors, setFieldErrors] = useState<FieldErrors>({})
  const assigning = isEditing ? status === "ASSIGNED" : true

  const buildRequest = (): DeviceCreate | DeviceUpdate => {
    const details = { imei: imei.trim(), manufacturer: manufacturer.trim(), model: model.trim() }
    const number = msisdn.trim() || undefined
    if (!isEditing) return { ...details, msisdn: number }
    // Any status other than Assigned releases the device from its line.
    return status === "ASSIGNED" ? { ...details, status, msisdn: number } : { ...details, status }
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
        <DialogTitle>{isEditing ? "Edit Device" : "Add Device"}</DialogTitle>
        <DialogDescription>
          {isEditing
            ? "Update the device, assign it to a line or release it back to stock."
            : "Register a handset. Enter a mobile number to assign it straight away."}
        </DialogDescription>
      </DialogHeader>
      <div className="grid gap-4 py-4">
        <TextField
          id="imei"
          label="IMEI"
          value={imei}
          onChange={setImei}
          error={fieldErrors.imei}
          inputMode="numeric"
          placeholder="15-digit IMEI"
          required
        />
        <div className="grid grid-cols-2 gap-4">
          <TextField
            id="manufacturer"
            label="Manufacturer"
            value={manufacturer}
            onChange={setManufacturer}
            error={fieldErrors.manufacturer}
            placeholder="e.g. Apple"
            required
          />
          <TextField
            id="model"
            label="Model"
            value={model}
            onChange={setModel}
            error={fieldErrors.model}
            placeholder="e.g. iPhone 16 Pro"
            required
          />
        </div>
        {isEditing && (
          <SelectField
            id="status"
            label="Status"
            value={status}
            options={options(DEVICE_STATUSES)}
            onChange={(v) => setStatus(v as DeviceStatus)}
            error={fieldErrors.status}
          />
        )}
        {assigning && (
          <TextField
            id="msisdn"
            label={isEditing ? "Mobile number" : "Mobile number (optional)"}
            value={msisdn}
            onChange={setMsisdn}
            error={fieldErrors.msisdn}
            placeholder="9123 4567"
            hint={isEditing ? undefined : "Leave empty to keep the device in stock."}
            required={isEditing}
          />
        )}
      </div>
      <FormError message={error} />
      <DialogFooter>
        <Button type="button" variant="outline" onClick={onCancel}>
          Cancel
        </Button>
        <Button type="submit" disabled={submitting}>
          {submitting ? "Saving..." : isEditing ? "Save Changes" : "Add Device"}
        </Button>
      </DialogFooter>
    </form>
  )
}

export function DeviceFormDialog({ open, onOpenChange, device, onSubmit }: DeviceFormDialogProps) {
  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-[480px]">
        {open && (
          <DeviceForm
            key={device?.id ?? "new"}
            device={device}
            onSubmit={onSubmit}
            onCancel={() => onOpenChange(false)}
          />
        )}
      </DialogContent>
    </Dialog>
  )
}
