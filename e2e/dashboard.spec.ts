import { test, expect, type Page } from "@playwright/test"
import { NO_SESSION, signInApi, signInUi } from "./support"

interface Overview {
  kpis: {
    monthly_revenue: number
    arpu: number | null
    active_lines: number
    breached_open_cases: number
  }
}

const money = (value: number) =>
  `S$${value.toLocaleString("en-SG", { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`

const kpi = (page: Page, name: string) => page.getByRole("group", { name })

test.describe("Dashboard", () => {
  test("shows the figures the API reports", async ({ page }) => {
    const api = await signInApi()
    const overview = (await (await api.get("/api/dashboard/overview")).json()) as Overview
    await api.dispose()

    await page.goto("/dashboard")

    await expect(kpi(page, "Monthly recurring revenue")).toContainText(
      money(overview.kpis.monthly_revenue)
    )
    await expect(kpi(page, "Monthly recurring revenue")).toContainText(
      `ARPU ${money(overview.kpis.arpu ?? 0)}`
    )
    await expect(kpi(page, "Active lines")).toContainText(
      overview.kpis.active_lines.toLocaleString("en-SG")
    )
    await expect(kpi(page, "Open cases")).toContainText(
      `${overview.kpis.breached_open_cases.toLocaleString("en-SG")} past their SLA`
    )

    const brandRows = page
      .getByRole("table", { name: "Revenue and ARPU by brand" })
      .getByRole("row")
    await expect(brandRows).toHaveCount(4)
    await expect(brandRows.nth(1)).toContainText("Singtel")
    await expect(brandRows.nth(2)).toContainText("GOMO")
    await expect(brandRows.nth(3)).toContainText("hi!Card")

    const segments = page.getByRole("table", { name: "Consumer and enterprise" })
    await expect(segments).toContainText("Consumer")
    await expect(segments).toContainText("Enterprise")
  })

  test("draws every chart", async ({ page }) => {
    await page.goto("/dashboard")

    for (const name of [
      "Revenue and ARPU by brand",
      "Line movement and churn",
      "Porting by operator",
      "Open cases by priority",
    ]) {
      const section = page.getByRole("region", { name })
      await expect(section.locator(".recharts-surface").first()).toBeVisible()
      // Empty bars are drawn as an empty group, so look for a drawn bar shape.
      await expect(section.locator("path.recharts-rectangle").first()).toBeVisible()
    }
    // The current month is only counted up to now, and its label says so.
    await expect(
      page.getByRole("region", { name: "Line movement and churn" }).getByText(/\d{4}\*$/)
    ).toBeVisible()
  })

  test("uses the Singtel branding", async ({ page }) => {
    await page.goto("/dashboard")

    await expect(page).toHaveTitle("Singtel Ops Console")
    const brand = page.getByRole("link", { name: /Singtel Ops Console/ })
    await expect(brand.getByRole("img", { name: "Singtel logo" })).toBeVisible()
    await expect(brand.getByText("Singtel", { exact: true })).toHaveCSS("color", "rgb(237, 28, 36)")
  })

  test("is open to every role", async ({ browser }) => {
    const context = await browser.newContext({ storageState: NO_SESSION })
    const page = await context.newPage()
    await signInUi(page, "product")
    await page.goto("/dashboard")
    await expect(kpi(page, "Monthly recurring revenue")).toBeVisible()
    await context.close()
  })
})
