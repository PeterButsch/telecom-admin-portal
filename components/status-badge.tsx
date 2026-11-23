import { label } from "@/lib/labels"
import { cn } from "@/lib/utils"

const TONES = {
  green: "bg-green-100 text-green-800 dark:bg-green-900/40 dark:text-green-200",
  blue: "bg-blue-100 text-blue-800 dark:bg-blue-900/40 dark:text-blue-200",
  yellow: "bg-yellow-100 text-yellow-800 dark:bg-yellow-900/40 dark:text-yellow-200",
  orange: "bg-orange-100 text-orange-800 dark:bg-orange-900/40 dark:text-orange-200",
  red: "bg-red-100 text-red-800 dark:bg-red-900/40 dark:text-red-200",
  purple: "bg-purple-100 text-purple-800 dark:bg-purple-900/40 dark:text-purple-200",
  gray: "bg-gray-100 text-gray-700 dark:bg-gray-800 dark:text-gray-300",
} as const

export type Tone = keyof typeof TONES

const VALUE_TONES: Record<string, Tone> = {
  ACTIVE: "green",
  INACTIVE: "gray",
  AVAILABLE: "green",
  RESOLVED: "green",
  OPEN: "blue",
  ASSIGNED: "blue",
  DATA: "purple",
  CALL: "blue",
  SMS: "green",
  IN_PROGRESS: "yellow",
  PENDING: "yellow",
  PENDING_CUSTOMER: "yellow",
  SUSPENDED: "yellow",
  MEDIUM: "yellow",
  HIGH: "orange",
  DAMAGED: "orange",
  BARRED: "red",
  LOST: "red",
  URGENT: "red",
  TERMINATED: "gray",
  CLOSED: "gray",
  LOW: "gray",
  SUCCESS: "green",
  FAILED: "red",
  DENIED: "orange",
  LOCKED: "red",
}

/** Small coloured label for an enum value such as a status or priority. */
export function StatusBadge({
  value,
  tone,
  children,
}: {
  value: string
  tone?: Tone
  children?: React.ReactNode
}) {
  return (
    <span
      className={cn(
        "inline-block rounded-full px-2 py-0.5 text-xs font-medium whitespace-nowrap",
        TONES[tone ?? VALUE_TONES[value] ?? "gray"]
      )}
    >
      {children ?? label(value)}
    </span>
  )
}
