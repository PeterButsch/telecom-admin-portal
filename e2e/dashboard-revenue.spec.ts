import { test, expect, type APIRequestContext } from "@playwright/test"
import { signInApi } from "./support"

interface Page<T> {
  content: T[]
  totalElements: number
  hasNext: boolean
}

interface ApiLine {
  status: string
  brand: string
  customer_type: string
  plan_name: string
  monthly_price: number
}

async function allPages<T>(request: APIRequestContext, path: string): Promise<T[]> {
  const items: T[] = []
  for (let page = 0; ; page++) {
    const separator = path.includes("?") ? "&" : "?"
    const res = await request.get(`${path}${separator}page=${page}&size=100`)
    expect(res.ok()).toBeTruthy()
    const body: Page<T> = await res.json()
    items.push(...body.content)
    if (!body.hasNext) return items
  }
}

async function total(request: APIRequestContext, path: string) {
  const res = await request.get(path)
  expect(res.ok()).toBeTruthy()
  return ((await res.json()) as Page<unknown>).totalElements
}

/**
 * Cross-checks the dashboard aggregates against the raw lists. The seeded data has suspended,
 * barred and terminated lines, so an aggregate that forgets to filter on ACTIVE shows up here.
 */
test.describe("Dashboard aggregates", () => {
  let api: APIRequestContext

  test.beforeAll(async () => {
    api = await signInApi()
  })

  test.afterAll(async () => {
    await api.dispose()
  })

  test("monthly revenue is the plan price of every active line", async () => {
    const stats = await (await api.get("/api/dashboard/stats")).json()
    const activeLines = await allPages<ApiLine>(api, "/api/lines?status=ACTIVE")

    expect(activeLines.every((line) => line.status === "ACTIVE")).toBeTruthy()
    const expected = activeLines.reduce((sum, line) => sum + Number(line.monthly_price), 0)
    expect(Number(stats.monthly_revenue)).toBeCloseTo(expected, 2)
    expect(stats.active_lines).toBe(activeLines.length)
  })

  test("revenue by plan matches the active lines on each plan", async () => {
    const revenue: { name: string; revenue: number }[] = await (
      await api.get("/api/dashboard/revenue-by-plan")
    ).json()
    const activeLines = await allPages<ApiLine>(api, "/api/lines?status=ACTIVE")

    const byPlan = new Map<string, number>()
    for (const line of activeLines) {
      byPlan.set(line.plan_name, (byPlan.get(line.plan_name) ?? 0) + Number(line.monthly_price))
    }
    expect(revenue.length).toBe(byPlan.size)
    for (const row of revenue) {
      expect(Number(row.revenue)).toBeCloseTo(byPlan.get(row.name) ?? -1, 2)
    }
  })

  test("customer and case counts match the filtered lists", async () => {
    const stats = await (await api.get("/api/dashboard/stats")).json()

    expect(stats.active_customers).toBe(await total(api, "/api/customers?status=ACTIVE"))
    const open = await Promise.all(
      ["OPEN", "IN_PROGRESS", "PENDING_CUSTOMER"].map((status) =>
        total(api, `/api/cases?status=${status}`)
      )
    )
    expect(stats.open_cases).toBe(open.reduce((a, b) => a + b, 0))
  })

  test("overview revenue and ARPU match the active lines by brand and segment", async () => {
    const overview = await (await api.get("/api/dashboard/overview")).json()
    const activeLines = await allPages<ApiLine>(api, "/api/lines?status=ACTIVE")

    const totals = (key: (line: ApiLine) => string) => {
      const result = new Map<string, { lines: number; revenue: number }>()
      for (const line of activeLines) {
        const row = result.get(key(line)) ?? { lines: 0, revenue: 0 }
        row.lines += 1
        row.revenue += Number(line.monthly_price)
        result.set(key(line), row)
      }
      return result
    }
    const check = (
      row: { active_lines: number; monthly_revenue: number; arpu: number | null },
      expected = { lines: 0, revenue: 0 }
    ) => {
      expect(row.active_lines).toBe(expected.lines)
      expect(Number(row.monthly_revenue)).toBeCloseTo(expected.revenue, 2)
      if (expected.lines > 0) {
        expect(Number(row.arpu)).toBeCloseTo(expected.revenue / expected.lines, 2)
      } else {
        expect(row.arpu).toBeNull()
      }
    }

    const byBrand = totals((line) => line.brand)
    expect(overview.brands.map((row: { brand: string }) => row.brand)).toEqual([
      "SINGTEL",
      "GOMO",
      "HI_CARD",
    ])
    for (const row of overview.brands) check(row, byBrand.get(row.brand))

    const byType = totals((line) => line.customer_type)
    const segmentType: Record<string, string> = { CONSUMER: "INDIVIDUAL", ENTERPRISE: "BUSINESS" }
    for (const row of overview.segments) check(row, byType.get(segmentType[row.segment] ?? ""))

    expect(overview.kpis.active_lines).toBe(activeLines.length)
    const stats = await (await api.get("/api/dashboard/stats")).json()
    expect(Number(overview.kpis.monthly_revenue)).toBeCloseTo(Number(stats.monthly_revenue), 2)
    expect(overview.kpis.open_cases).toBe(stats.open_cases)
  })
})
