import { test, expect } from "@playwright/test"
import { DEMO_PASSWORD, NO_SESSION, signInUi } from "./support"

test.use({ storageState: NO_SESSION })

test.describe("Sign-in and roles", () => {
  test("sends visitors to sign in and back to the page they asked for", async ({ page }) => {
    await page.goto("/customers")
    await expect(page).toHaveURL(/\/login\?next=%2Fcustomers/)
    await expect(page.getByText(DEMO_PASSWORD)).toBeVisible()

    await page.getByRole("button", { name: /Sign in as Priya Nair/ }).click()

    await expect(page).toHaveURL(/\/customers$/)
    await expect(page.locator("table tbody tr").first()).toBeVisible()
  })

  test("care agents can edit customers but not delete them or administer staff", async ({
    page,
  }) => {
    await signInUi(page, "agent")
    await page.goto("/customers")
    await expect(page.locator("table tbody tr").first()).toBeVisible()

    await expect(page.getByRole("button", { name: /add customer/i })).toBeVisible()
    await expect(page.getByRole("button", { name: /^Edit / }).first()).toBeVisible()
    await expect(page.getByRole("button", { name: /^Delete / })).toHaveCount(0)
    await expect(page.getByRole("link", { name: "Staff Accounts" })).toHaveCount(0)
    await expect(page.getByRole("link", { name: "Audit Log" })).toHaveCount(0)

    await page.goto("/staff")
    await expect(page.getByText("You do not have access to this page")).toBeVisible()
  })

  test("product managers see the catalogue but no customer records", async ({ page }) => {
    await signInUi(page, "product")
    await page.goto("/plans")
    await expect(page.getByRole("button", { name: /add plan/i })).toBeVisible()
    await expect(page.getByRole("link", { name: "Customers" })).toHaveCount(0)

    const customerRequests: string[] = []
    page.on("request", (req) => {
      if (req.url().includes("/api/customers")) customerRequests.push(req.url())
    })
    await page.goto("/customers")
    await expect(page.getByText("You do not have access to this page")).toBeVisible()
    expect(customerRequests).toEqual([])
  })

  test("care agents can edit devices but not delete them", async ({ page }) => {
    await signInUi(page, "agent")
    await page.goto("/devices")
    await expect(page.locator("table tbody tr").first()).toBeVisible()
    await expect(page.getByRole("columnheader", { name: "Customer" })).toBeVisible()
    await expect(page.getByRole("button", { name: /^Edit device / }).first()).toBeVisible()
    await expect(page.getByRole("button", { name: /^Delete device / })).toHaveCount(0)
  })

  test("product managers see devices without lines or customers", async ({ page }) => {
    await signInUi(page, "product")
    await page.goto("/devices")
    await expect(page.locator("table tbody tr").first()).toBeVisible()
    await expect(page.getByRole("columnheader", { name: "IMEI" })).toBeVisible()
    await expect(page.getByRole("columnheader", { name: "Customer" })).toHaveCount(0)
    await expect(page.getByRole("columnheader", { name: "Mobile number" })).toHaveCount(0)
  })

  test("network operations see lines by account, without customer names", async ({ page }) => {
    await signInUi(page, "netops")
    await page.goto("/lines")
    await expect(page.locator("table tbody tr").first()).toBeVisible()
    await expect(page.getByRole("columnheader", { name: "Account" })).toBeVisible()
    await expect(page.getByRole("columnheader", { name: "Customer" })).toHaveCount(0)
    await expect(page.getByLabel("Search")).toHaveAttribute(
      "placeholder",
      "Mobile number, account no. or ICCID"
    )
  })

  test("rejects an incorrect password", async ({ page }) => {
    await page.goto("/login")
    await page.getByLabel("Username").fill("nobody.e2e")
    await page.getByLabel("Password").fill("not-the-password-1")
    await page.getByRole("button", { name: "Sign in", exact: true }).click()

    await expect(
      page.getByRole("alert").filter({ hasText: "Invalid username or password." })
    ).toBeVisible()
    await expect(page).toHaveURL(/\/login/)
  })

  test("signing out ends the session", async ({ page }) => {
    await signInUi(page, "fraud")
    await page.getByRole("button", { name: /Account menu for Siti Rahman/ }).click()
    await page.getByRole("menuitem", { name: "Sign out" }).click()
    await expect(page).toHaveURL(/\/login$/)

    await page.goto("/cases")
    await expect(page).toHaveURL(/\/login\?next=%2Fcases/)
  })
})
