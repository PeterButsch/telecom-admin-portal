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
import { label, options } from "@/lib/labels"
import {
  CUSTOMER_STATUSES,
  CUSTOMER_TYPES,
  ID_TYPES,
  type Customer,
  type CustomerCreate,
  type CustomerStatus,
  type CustomerType,
  type CustomerUpdate,
  type IdType,
} from "@/hooks/use-customers"

interface CustomerFormDialogProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  customer: Customer | null
  onSubmit: (data: CustomerCreate | CustomerUpdate) => Promise<void>
}

const optional = (value: string) => value.trim() || undefined

function CustomerForm({
  customer,
  onSubmit,
  onCancel,
}: {
  customer: Customer | null
  onSubmit: (data: CustomerCreate | CustomerUpdate) => Promise<void>
  onCancel: () => void
}) {
  const isEditing = customer !== null
  const [type, setType] = useState<CustomerType>(customer?.type ?? "INDIVIDUAL")
  const [firstName, setFirstName] = useState(customer?.first_name ?? "")
  const [lastName, setLastName] = useState(customer?.last_name ?? "")
  const [companyName, setCompanyName] = useState(customer?.company_name ?? "")
  const [uen, setUen] = useState("")
  const [contactName, setContactName] = useState(customer?.contact_name ?? "")
  const [idType, setIdType] = useState<IdType>("NRIC")
  const [idNumber, setIdNumber] = useState("")
  const [dateOfBirth, setDateOfBirth] = useState("")
  const [email, setEmail] = useState(customer?.email ?? "")
  const [phone, setPhone] = useState(customer?.phone ?? "")
  const [addressLine, setAddressLine] = useState(customer?.address_line ?? "")
  const [postalCode, setPostalCode] = useState(customer?.postal_code ?? "")
  const [status, setStatus] = useState<CustomerStatus>(customer?.status ?? "ACTIVE")
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [fieldErrors, setFieldErrors] = useState<FieldErrors>({})
  const business = type === "BUSINESS"

  const buildRequest = (): CustomerCreate | CustomerUpdate => {
    const contact = {
      email: email.trim(),
      phone: phone.trim(),
      addressLine: addressLine.trim(),
      postalCode: postalCode.trim(),
    }
    const names = business
      ? { companyName: companyName.trim(), contactName: contactName.trim() }
      : { firstName: firstName.trim(), lastName: lastName.trim() }
    if (isEditing) {
      return { ...names, ...contact, status }
    }
    const identity = business
      ? { uen: uen.trim() }
      : { idType, idNumber: idNumber.trim(), dateOfBirth: optional(dateOfBirth) }
    return {
      type,
      ...names,
      ...identity,
      ...contact,
      addressLine: optional(addressLine),
      postalCode: optional(postalCode),
    }
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
        <DialogTitle>{isEditing ? "Edit Customer" : "Add Customer"}</DialogTitle>
        <DialogDescription>
          {isEditing
            ? `Update the contact details for ${customer.customer_number}. ID details cannot be changed.`
            : "Register a new consumer or business customer."}
        </DialogDescription>
      </DialogHeader>
      <div className="grid gap-4 py-4">
        {isEditing ? (
          <div className="grid grid-cols-2 gap-4">
            <ReadOnlyField label="Customer type" value={label(customer.type)} />
            <ReadOnlyField
              label={business ? "UEN" : "ID"}
              value={
                business
                  ? (customer.uen ?? "-")
                  : `${label(customer.id_type)} ${customer.id_number_masked ?? ""}`
              }
            />
          </div>
        ) : (
          <SelectField
            id="type"
            label="Customer type"
            value={type}
            options={options(CUSTOMER_TYPES)}
            onChange={(v) => setType(v as CustomerType)}
          />
        )}

        {business ? (
          <>
            <TextField
              id="companyName"
              label="Company name"
              value={companyName}
              onChange={setCompanyName}
              error={fieldErrors.companyName}
              required
            />
            <div className="grid grid-cols-2 gap-4">
              {!isEditing && (
                <TextField
                  id="uen"
                  label="UEN"
                  value={uen}
                  onChange={setUen}
                  error={fieldErrors.uen}
                  placeholder="201912345K"
                  required
                />
              )}
              <TextField
                id="contactName"
                label="Contact person"
                value={contactName}
                onChange={setContactName}
                error={fieldErrors.contactName}
              />
            </div>
          </>
        ) : (
          <>
            <div className="grid grid-cols-2 gap-4">
              <TextField
                id="firstName"
                label="First name"
                value={firstName}
                onChange={setFirstName}
                error={fieldErrors.firstName}
                required
              />
              <TextField
                id="lastName"
                label="Last name"
                value={lastName}
                onChange={setLastName}
                error={fieldErrors.lastName}
                required
              />
            </div>
            {!isEditing && (
              <div className="grid grid-cols-3 gap-4">
                <SelectField
                  id="idType"
                  label="ID type"
                  value={idType}
                  options={options(ID_TYPES)}
                  onChange={(v) => setIdType(v as IdType)}
                  error={fieldErrors.idType}
                />
                <TextField
                  id="idNumber"
                  label="ID number"
                  value={idNumber}
                  onChange={setIdNumber}
                  error={fieldErrors.idNumber}
                  autoComplete="off"
                  required
                />
                <TextField
                  id="dateOfBirth"
                  label="Date of birth"
                  type="date"
                  value={dateOfBirth}
                  onChange={setDateOfBirth}
                  error={fieldErrors.dateOfBirth}
                  required
                />
              </div>
            )}
          </>
        )}

        <div className="grid grid-cols-2 gap-4">
          <TextField
            id="email"
            label="Email"
            type="email"
            value={email}
            onChange={setEmail}
            error={fieldErrors.email}
            required
          />
          <TextField
            id="phone"
            label="Contact number"
            value={phone}
            onChange={setPhone}
            error={fieldErrors.phone}
            placeholder="9123 4567"
            required
          />
        </div>
        <div className="grid grid-cols-3 gap-4">
          <TextField
            id="addressLine"
            label="Address"
            className="col-span-2"
            value={addressLine}
            onChange={setAddressLine}
            error={fieldErrors.addressLine}
          />
          <TextField
            id="postalCode"
            label="Postal code"
            value={postalCode}
            onChange={setPostalCode}
            error={fieldErrors.postalCode}
            inputMode="numeric"
            placeholder="123456"
          />
        </div>
        {isEditing && (
          <SelectField
            id="status"
            label="Status"
            value={status}
            options={options(CUSTOMER_STATUSES)}
            onChange={(v) => setStatus(v as CustomerStatus)}
            error={fieldErrors.status}
          />
        )}
      </div>
      <FormError message={error} />
      <DialogFooter>
        <Button type="button" variant="outline" onClick={onCancel}>
          Cancel
        </Button>
        <Button type="submit" disabled={submitting}>
          {submitting ? "Saving..." : isEditing ? "Save Changes" : "Add Customer"}
        </Button>
      </DialogFooter>
    </form>
  )
}

export function CustomerFormDialog({
  open,
  onOpenChange,
  customer,
  onSubmit,
}: CustomerFormDialogProps) {
  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-[560px]">
        {open && (
          <CustomerForm
            key={customer?.id ?? "new"}
            customer={customer}
            onSubmit={onSubmit}
            onCancel={() => onOpenChange(false)}
          />
        )}
      </DialogContent>
    </Dialog>
  )
}
