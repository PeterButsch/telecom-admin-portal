import { test, expect } from "@playwright/test"

const listPages = [
  "/customers",
  "/lines",
  "/plans",
  "/devices",
  "/usage",
  "/cases",
  "/staff",
  "/audit",
]

test.describe("Navigation", () => {
  test("should navigate to all main pages", async ({ page }) => {
    await page.goto("/")
    await expect(page).toHaveTitle(/Telecom Demo/)

    await page.goto("/dashboard")
    await expect(page.getByLabel("breadcrumb")).toBeVisible()

    for (const path of listPages) {
      await page.goto(path)
      await expect(page.locator("table")).toBeVisible()
    }
  })

  test("sidebar navigation works correctly", async ({ page }) => {
    await page.goto("/dashboard")

    await page.getByRole("link", { name: /customers/i }).click()
    await expect(page).toHaveURL(/.*customers/)

    await page.getByRole("link", { name: /mobile lines/i }).click()
    await expect(page).toHaveURL(/.*lines/)

    await page.getByRole("link", { name: /plans/i }).click()
    await expect(page).toHaveURL(/.*plans/)

    await page.getByRole("link", { name: /support cases/i }).click()
    await expect(page).toHaveURL(/.*cases/)

    await page.getByRole("link", { name: /audit log/i }).click()
    await expect(page).toHaveURL(/.*audit/)
  })
})
