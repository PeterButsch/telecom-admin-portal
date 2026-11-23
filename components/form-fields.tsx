"use client"

import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select"
import { ApiError, errorMessage } from "@/lib/api"
import type { Option } from "@/lib/labels"
import { cn } from "@/lib/utils"

export type FieldErrors = Record<string, string>

/** Splits a failed save into per-field messages and a message for the whole form. */
export function describeSaveError(e: unknown): { form: string; fields: FieldErrors } {
  if (e instanceof ApiError && Object.keys(e.fieldErrors).length > 0) {
    return { form: "Please correct the highlighted fields.", fields: e.fieldErrors }
  }
  return { form: errorMessage(e), fields: {} }
}

function FieldMessage({ id, message }: { id: string; message?: string }) {
  if (!message) return null
  return (
    <p id={`${id}-error`} className="text-destructive text-xs">
      {message}
    </p>
  )
}

interface TextFieldProps extends Omit<React.ComponentProps<typeof Input>, "onChange"> {
  id: string
  label: string
  value: string
  onChange: (value: string) => void
  error?: string
  hint?: string
}

export function TextField({
  id,
  label,
  value,
  onChange,
  error,
  hint,
  className,
  ...inputProps
}: TextFieldProps) {
  return (
    <div className={cn("space-y-2", className)}>
      <Label htmlFor={id}>{label}</Label>
      <Input
        id={id}
        value={value}
        onChange={(e) => onChange(e.target.value)}
        aria-invalid={error ? true : undefined}
        aria-describedby={error ? `${id}-error` : undefined}
        {...inputProps}
      />
      {hint && !error && <p className="text-muted-foreground text-xs">{hint}</p>}
      <FieldMessage id={id} message={error} />
    </div>
  )
}

interface SelectFieldProps {
  id: string
  label: string
  value: string
  options: Option[]
  onChange: (value: string) => void
  error?: string
  placeholder?: string
  disabled?: boolean
  className?: string
}

export function SelectField({
  id,
  label,
  value,
  options,
  onChange,
  error,
  placeholder,
  disabled,
  className,
}: SelectFieldProps) {
  return (
    <div className={cn("space-y-2", className)}>
      <Label htmlFor={id}>{label}</Label>
      <Select value={value} onValueChange={onChange} disabled={disabled}>
        <SelectTrigger
          id={id}
          className="w-full"
          aria-invalid={error ? true : undefined}
          aria-describedby={error ? `${id}-error` : undefined}
        >
          <SelectValue placeholder={placeholder} />
        </SelectTrigger>
        <SelectContent>
          {options.map((option) => (
            <SelectItem key={option.value} value={option.value}>
              {option.label}
            </SelectItem>
          ))}
        </SelectContent>
      </Select>
      <FieldMessage id={id} message={error} />
    </div>
  )
}

/** A value shown in a form that cannot be changed. */
export function ReadOnlyField({ label, value }: { label: string; value: React.ReactNode }) {
  return (
    <div className="space-y-2">
      <div className="text-sm leading-none font-medium">{label}</div>
      <div className="text-muted-foreground text-sm">{value}</div>
    </div>
  )
}
