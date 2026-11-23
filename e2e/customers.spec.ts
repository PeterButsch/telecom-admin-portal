import { test, expect } from "@playwright/test"

test.describe("Customers Page", () => {
  test.beforeEach(async ({ page }) => {
    await page.goto("/customers")
  })

  test("should display customers table", async ({ page }) => {
    await expect(page.getByLabel("breadcrumb")).toBeVisible()
    await expect(page.locator("table")).toBeVisible()
  })

  test("should load a page of customers from the API", async ({ page }) => {
    const customersResponse = page.waitForResponse(
      (response) => response.url().includes("/api/customers") && response.status() === 200
    )
    await page.reload()
    const data = await (await customersResponse).json()
    expect(Array.isArray(data.content)).toBeTruthy()
    expect(data.totalElements).toBeGreaterThan(0)
  })

  test("should display customer data in table rows", async ({ page }) => {
    await page.waitForSelector("table tbody tr", { timeout: 10000 })
    expect(await page.locator("table tbody tr").count()).toBeGreaterThan(0)
  })

  test("never shows a full NRIC in the list", async ({ page }) => {
    await page.waitForSelector("table tbody tr", { timeout: 10000 })
    const text = await page.locator("table tbody").innerText()
    expect(text).not.toMatch(/\b[STFGM]\d{7}[A-Z]\b/)
    expect(text).toMatch(/NRIC \*+\d{3}[A-Z]/)
  })

  test("searches customers by name", async ({ page }) => {
    await page.waitForSelector("table tbody tr", { timeout: 10000 })
    const firstName = (
      await page.locator("table tbody tr td").nth(1).locator("div").first().innerText()
    )
      .split(" ")
      .at(-1) as string

    const searched = page.waitForResponse(
      (response) => response.url().includes(`q=${encodeURIComponent(firstName)}`) && response.ok()
    )
    await page.getByLabel("Search").fill(firstName)
    await searched

    const rows = page.locator("table tbody tr")
    await expect(rows.first()).toContainText(new RegExp(firstName, "i"))
  })

  test("should have add customer button", async ({ page }) => {
    await expect(page.getByRole("button", { name: /add customer/i })).toBeVisible()
  })
})
