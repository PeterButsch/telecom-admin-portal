"use client"

import { Label } from "@/components/ui/label"
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select"
import type { Option } from "@/lib/labels"

const ALL = "all"

/** Labelled filter drop-down whose first entry clears the filter. */
export function FilterSelect({
  id,
  label,
  value,
  options,
  onChange,
  allLabel = "All",
  className = "w-[150px]",
}: {
  id: string
  label: string
  value: string | null | undefined
  options: Option[]
  onChange: (value: string | null) => void
  allLabel?: string
  className?: string
}) {
  return (
    <div className="space-y-1">
      <Label htmlFor={id}>{label}</Label>
      <Select value={value || ALL} onValueChange={(v) => onChange(v === ALL ? null : v)}>
        <SelectTrigger id={id} className={className}>
          <SelectValue />
        </SelectTrigger>
        <SelectContent>
          <SelectItem value={ALL}>{allLabel}</SelectItem>
          {options.map((option) => (
            <SelectItem key={option.value} value={option.value}>
              {option.label}
            </SelectItem>
          ))}
        </SelectContent>
      </Select>
    </div>
  )
}
