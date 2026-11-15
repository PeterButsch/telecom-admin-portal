import { test, expect, type APIRequestContext } from "@playwright/test"
import { signInApi } from "./support"

interface Page<T> {
  content: T[]
  totalElements: number
  hasNext: boolean
}

interface ApiLine {
  status: string
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
})
