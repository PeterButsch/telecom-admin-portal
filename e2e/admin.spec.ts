import { test, expect } from "@playwright/test"

test.describe("Administration", () => {
  test("lists the staff accounts with their roles", async ({ page }) => {
    await page.goto("/staff")
    const table = page.locator("table tbody")
    await expect(table).toContainText("Grace Lim")
    await expect(table).toContainText("(you)")
    await expect(table).toContainText("Fraud & Compliance Analyst")
    await expect(table).toContainText("Product Manager")
  })

  test("the audit log records sign-ins", async ({ page }) => {
    await page.goto("/audit")
    await expect(page.locator("table tbody tr").first()).toBeVisible()

    const filtered = page.waitForResponse(
      (response) => /[?&]action=LOGIN(&|$)/.test(response.url()) && response.ok()
    )
    await page.getByLabel("Action").click()
    await page.getByRole("option", { name: "Login", exact: true }).click()
    await filtered

    const rows = page.locator("table tbody tr")
    await expect(rows.first()).toContainText("Login")
    await expect(rows.first()).toContainText("Success")
  })
})
