const SPECIAL_LABELS: Record<string, string> = {
  SINGTEL: "Singtel",
  GOMO: "GOMO",
  HI_CARD: "hi!Card",
  SIM_ONLY: "SIM only",
  DATA_ONLY: "Data only",
  ESIM: "eSIM",
  PHYSICAL: "Physical SIM",
  STARHUB: "StarHub",
  CIRCLES_LIFE: "Circles.Life",
  SIMBA: "SIMBA",
  M1: "M1",
  NRIC: "NRIC",
  FIN: "FIN",
  SMS: "SMS",
  GIRO: "GIRO",
  PAYNOW: "PayNow",
  TOP_UP: "Top-up",
  SCAM_REPORT: "Scam report",
  SIM_REPLACEMENT: "SIM replacement",
  APP: "App",
  CARE_AGENT: "Care Agent",
  CARE_TEAM_LEAD: "Care Team Lead",
  FRAUD_ANALYST: "Fraud & Compliance Analyst",
  NETWORK_OPS: "Network Operations",
  PRODUCT_MANAGER: "Product Manager",
  ADMIN: "Administrator",
}

/** Turns an API enum value such as "PENDING_CUSTOMER" into a readable label ("Pending customer"). */
export function label(value: string | null | undefined): string {
  if (!value) return "-"
  const special = SPECIAL_LABELS[value]
  if (special) return special
  const words = value.toLowerCase().replace(/_/g, " ")
  return words.charAt(0).toUpperCase() + words.slice(1)
}

export interface Option {
  value: string
  label: string
}

export function options(values: readonly string[]): Option[] {
  return values.map((value) => ({ value, label: label(value) }))
}
