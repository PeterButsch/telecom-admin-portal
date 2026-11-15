import { describe, it, expect } from "vitest"
import {
  formatCount,
  formatDate,
  formatDateTime,
  formatMoney,
  formatMonth,
  formatMsisdn,
  formatPercent,
  formatShortDateTime,
  formatSigned,
  formatUsage,
} from "@/lib/format"
import { label, options } from "@/lib/labels"

describe("formatMoney", () => {
  it("formats Singapore dollars with two decimals and thousands separators", () => {
    expect(formatMoney(1234.5)).toBe("S$1,234.50")
    expect(formatMoney(0)).toBe("S$0.00")
  })

  it("puts the minus sign before the currency", () => {
    expect(formatMoney(-5)).toBe("-S$5.00")
  })

  it("shows a dash for missing values", () => {
    expect(formatMoney(null)).toBe("-")
    expect(formatMoney(undefined)).toBe("-")
  })
})

describe("formatMsisdn", () => {
  it("groups an 8-digit number as 4 + 4", () => {
    expect(formatMsisdn("91234567")).toBe("9123 4567")
  })

  it("leaves other values alone", () => {
    expect(formatMsisdn("+6591234567")).toBe("+6591234567")
    expect(formatMsisdn(null)).toBe("-")
  })
})

describe("formatDate and formatDateTime", () => {
  it("shows plain dates as the same calendar day", () => {
    expect(formatDate("2026-03-01")).toBe("1 Mar 2026")
  })

  it("converts timestamps to Singapore time", () => {
    // 20:30 UTC is 04:30 the next day in Singapore (UTC+8).
    expect(formatDate("2026-03-01T20:30:00Z")).toBe("2 Mar 2026")
    expect(formatDateTime("2026-03-01T20:30:00Z")).toContain("04:30")
    expect(formatShortDateTime("2026-03-01T20:30:00Z")).toMatch(/^2 Mar,? 04:30$/)
  })

  it("shows a dash for missing or invalid values", () => {
    expect(formatDate(null)).toBe("-")
    expect(formatDateTime("not a date")).toBe("-")
  })
})

describe("formatUsage", () => {
  it("shows large data volumes in GB", () => {
    expect(formatUsage(1536, "MB")).toBe("1.5 GB")
    expect(formatUsage(512, "MB")).toBe("512 MB")
  })

  it("labels minutes and SMS", () => {
    expect(formatUsage(12, "MIN")).toBe("12 min")
    expect(formatUsage(3, "SMS")).toBe("3 SMS")
  })
})

describe("label", () => {
  it("turns enum values into sentence case", () => {
    expect(label("PENDING_CUSTOMER")).toBe("Pending customer")
    expect(label("ACTIVE")).toBe("Active")
  })

  it("uses brand and product spellings", () => {
    expect(label("HI_CARD")).toBe("hi!Card")
    expect(label("GOMO")).toBe("GOMO")
    expect(label("ESIM")).toBe("eSIM")
    expect(label("NRIC")).toBe("NRIC")
  })

  it("builds select options", () => {
    expect(options(["SINGTEL", "SIM_ONLY"])).toEqual([
      { value: "SINGTEL", label: "Singtel" },
      { value: "SIM_ONLY", label: "SIM only" },
    ])
  })
})

describe("dashboard figures", () => {
  it("formats counts and signed changes", () => {
    expect(formatCount(1774)).toBe("1,774")
    expect(formatCount(null)).toBe("-")
    expect(formatSigned(12)).toBe("+12")
    expect(formatSigned(-1500)).toBe("-1,500")
    expect(formatSigned(0)).toBe("0")
  })

  it("formats rates and shows a dash when there is none", () => {
    expect(formatPercent(66.666, 2)).toBe("66.67%")
    expect(formatPercent(50)).toBe("50.0%")
    expect(formatPercent(null)).toBe("-")
  })

  it("names calendar months", () => {
    expect(formatMonth("2026-02")).toBe("Feb 2026")
    expect(formatMonth("2025-12")).toBe("Dec 2025")
    expect(formatMonth("2025-13")).toBe("2025-13")
  })
})
