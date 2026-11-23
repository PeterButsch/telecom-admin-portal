import { test, expect } from "@playwright/test"

test.describe("Plans Page", () => {
  test.beforeEach(async ({ page }) => {
    await page.goto("/plans")
  })

  test("should display plans table", async ({ page }) => {
    await expect(page.getByLabel("breadcrumb")).toBeVisible()
    await expect(page.locator("table")).toBeVisible()
  })

  test("should load plans from API", async ({ page }) => {
    const plansResponse = page.waitForResponse(
      (response) => response.url().includes("/api/plans") && response.status() === 200
    )
    await page.reload()
    const data = await (await plansResponse).json()
    expect(Array.isArray(data)).toBeTruthy()
  })

  test("should list all three brands with Singapore dollar prices", async ({ page }) => {
    await page.waitForSelector("table tbody tr", { timeout: 10000 })
    const body = page.locator("table tbody")
    await expect(body).toContainText("Singtel")
    await expect(body).toContainText("GOMO")
    await expect(body).toContainText("hi!Card")
    expect(
      await page.locator("table tbody tr td").filter({ hasText: "S$" }).count()
    ).toBeGreaterThan(0)
  })
})
