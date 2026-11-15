import type { Brand, Segment } from "@/hooks/use-dashboard-overview"

/** Chart colours for each brand and segment, so every chart uses the same one. */
export const BRAND_COLOURS: Record<Brand, string> = {
  SINGTEL: "var(--chart-1)",
  GOMO: "var(--chart-2)",
  HI_CARD: "var(--chart-4)",
}

export const SEGMENT_COLOURS: Record<Segment, string> = {
  CONSUMER: "var(--chart-1)",
  ENTERPRISE: "var(--chart-5)",
}

/** A part's share of a whole as a percentage, or null when the whole is zero. */
export function share(part: number, whole: number): number | null {
  return whole === 0 ? null : (part / whole) * 100
}
