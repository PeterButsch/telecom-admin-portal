const SINGAPORE = "Asia/Singapore"

const amount = new Intl.NumberFormat("en-SG", {
  minimumFractionDigits: 2,
  maximumFractionDigits: 2,
})
const dateFormat = new Intl.DateTimeFormat("en-SG", {
  timeZone: SINGAPORE,
  day: "numeric",
  month: "short",
  year: "numeric",
})
const dateTimeFormat = new Intl.DateTimeFormat("en-SG", {
  timeZone: SINGAPORE,
  day: "numeric",
  month: "short",
  year: "numeric",
  hour: "2-digit",
  minute: "2-digit",
  hour12: false,
})

/** Singapore dollars, for example "S$1,234.50" or "-S$5.00". */
export function formatMoney(value: number | null | undefined): string {
  if (value === null || value === undefined || Number.isNaN(value)) return "-"
  const formatted = `S$${amount.format(Math.abs(value))}`
  return value < 0 ? `-${formatted}` : formatted
}

/** Shows an 8-digit Singapore mobile number as "9123 4567". */
export function formatMsisdn(msisdn: string | null | undefined): string {
  if (!msisdn) return "-"
  return /^\d{8}$/.test(msisdn) ? `${msisdn.slice(0, 4)} ${msisdn.slice(4)}` : msisdn
}

/** Calendar date in Singapore time. Accepts ISO timestamps and plain "yyyy-mm-dd" dates. */
export function formatDate(value: string | null | undefined): string {
  if (!value) return "-"
  // A plain date has no time zone; pin it to midday UTC so it shows as the same day everywhere.
  const date = new Date(/^\d{4}-\d{2}-\d{2}$/.test(value) ? `${value}T12:00:00Z` : value)
  return Number.isNaN(date.getTime()) ? "-" : dateFormat.format(date)
}

export function formatDateTime(value: string | null | undefined): string {
  if (!value) return "-"
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? "-" : dateTimeFormat.format(date)
}

const shortDateTimeFormat = new Intl.DateTimeFormat("en-SG", {
  timeZone: SINGAPORE,
  day: "numeric",
  month: "short",
  hour: "2-digit",
  minute: "2-digit",
  hour12: false,
})

/** Day, month and time without the year, for recent events such as SLA deadlines. */
export function formatShortDateTime(value: string | null | undefined): string {
  if (!value) return "-"
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? "-" : shortDateTimeFormat.format(date)
}

/** Formats a usage quantity with its unit, for example "1.5 GB" for 1536 MB. */
export function formatUsage(quantity: number, unit: string): string {
  if (unit === "MB" && quantity >= 1024) {
    return `${(quantity / 1024).toFixed(1)} GB`
  }
  if (unit === "MIN") return `${quantity} min`
  return `${quantity} ${unit}`
}

const count = new Intl.NumberFormat("en-SG")
const MONTH_NAMES = [
  "Jan",
  "Feb",
  "Mar",
  "Apr",
  "May",
  "Jun",
  "Jul",
  "Aug",
  "Sep",
  "Oct",
  "Nov",
  "Dec",
]

/** A whole number with thousands separators, for example "1,774". */
export function formatCount(value: number | null | undefined): string {
  if (value === null || value === undefined || Number.isNaN(value)) return "-"
  return count.format(value)
}

/** A change with its sign, for example "+12", "-3" or "0". */
export function formatSigned(value: number): string {
  return value > 0 ? `+${count.format(value)}` : count.format(value)
}

/** A percentage such as "2.45%", or a dash when there is no rate (nothing to divide by). */
export function formatPercent(value: number | null | undefined, digits = 1): string {
  if (value === null || value === undefined || Number.isNaN(value)) return "-"
  return `${value.toFixed(digits)}%`
}

/** A "yyyy-mm" calendar month as "Feb 2026". */
export function formatMonth(month: string): string {
  const match = /^(\d{4})-(\d{2})$/.exec(month)
  const name = match ? MONTH_NAMES[Number(match[2]) - 1] : undefined
  return match && name ? `${name} ${match[1]}` : month
}
