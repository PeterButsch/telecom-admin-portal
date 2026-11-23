import { test, expect, type Page } from "@playwright/test"
import { NO_SESSION, signInApi, signInUi } from "./support"

interface Insights {
  alerts: {
    id: string
    area: "USAGE" | "PLANS"
    title: string
    affected: number
    lines: unknown[] | null
  }[]
}

async function insightsAs(username: string): Promise<Insights> {
  const api = await signInApi(username)
  const response = await api.get("/api/dashboard/insights")
  expect(response.ok()).toBeTruthy()
  const insights = (await response.json()) as Insights
  await api.dispose()
  return insights
}

const alertList = (page: Page) => page.getByRole("list", { name: "Alerts" })

test.describe("Alerts & Insights", () => {
  test("lists the alerts the API reports and opens their lines", async ({ page }) => {
    const insights = await insightsAs("admin")
    expect(insights.alerts.length).toBeGreaterThan(0)

    await page.goto("/dashboard")
    await page.getByRole("link", { name: "Alerts & Insights" }).click()
    await expect(page).toHaveURL(/\/insights$/)

    const items = alertList(page).getByRole("listitem")
    await expect(items).toHaveCount(insights.alerts.length)
    const first = insights.alerts[0]!
    await expect(items.first()).toContainText(first.title)

    await items
      .first()
      .getByRole("button", { name: /^Show / })
      .click()
    const lines = page.getByRole("table", { name: `Lines for: ${first.title}` })
    await expect(lines.getByRole("row")).toHaveCount(first.affected + 1)
    await expect(lines.getByRole("columnheader", { name: "Customer" })).toBeVisible()
  })

  test("filters by area and severity", async ({ page }) => {
    const insights = await insightsAs("admin")
    const plans = insights.alerts.filter((alert) => alert.area === "PLANS")

    await page.goto("/insights")
    await expect(alertList(page).getByRole("listitem")).toHaveCount(insights.alerts.length)

    await page.getByLabel("Area").click()
    await page.getByRole("option", { name: "Plans", exact: true }).click()
    await expect(alertList(page).getByRole("listitem")).toHaveCount(plans.length)
    await expect(
      page.getByText(`${plans.length} of ${insights.alerts.length} alerts`)
    ).toBeVisible()

    await page.getByLabel("Severity").click()
    await page.getByRole("option", { name: "High", exact: true }).click()
    // Only the roaming usage check raises high alerts.
    await expect(page.getByText("No alerts match these filters.")).toBeVisible()
  })

  test("product managers see plan alerts as counts only", async ({ browser }) => {
    const insights = await insightsAs("product")
    expect(insights.alerts.every((alert) => alert.area === "PLANS" && alert.lines === null)).toBe(
      true
    )

    const context = await browser.newContext({ storageState: NO_SESSION })
    const page = await context.newPage()
    await signInUi(page, "product")
    await page.goto("/insights")

    await expect(alertList(page).getByRole("listitem")).toHaveCount(insights.alerts.length)
    await expect(alertList(page).getByRole("button")).toHaveCount(0)
    await expect(alertList(page).getByText("Usage", { exact: true })).toHaveCount(0)
    await context.close()
  })

  test("network operations see lines without customers", async ({ browser }) => {
    const context = await browser.newContext({ storageState: NO_SESSION })
    const page = await context.newPage()
    await signInUi(page, "netops")
    await page.goto("/insights")

    const first = alertList(page).getByRole("listitem").first()
    await first.getByRole("button", { name: /^Show / }).click()
    const table = first.getByRole("table")
    await expect(table.getByRole("columnheader", { name: "Mobile number" })).toBeVisible()
    await expect(table.getByRole("columnheader", { name: "Customer" })).toHaveCount(0)
    await context.close()
  })
})
