import { test, expect, type Page } from "@playwright/test"

/** Scrolls the list's scroll container (the element useInfiniteScroll listens to) to the bottom. */
async function scrollListToBottom(page: Page) {
  await page.locator("table").evaluate((table) => {
    let el = table.parentElement
    while (el && !(el.classList.contains("h-full") && el.classList.contains("overflow-auto"))) {
      el = el.parentElement
    }
    if (!el) throw new Error("No list scroll container found")
    el.scrollTop = el.scrollHeight
  })
}

function linesPage(page: Page, brand: string | null, pageNumber: number) {
  return page.waitForResponse((response) => {
    const url = new URL(response.url())
    return (
      url.pathname === "/api/lines" &&
      url.searchParams.get("page") === String(pageNumber) &&
      url.searchParams.get("brand") === brand &&
      response.ok()
    )
  })
}

test.describe("Lists", () => {
  test("keep loading more rows after a filter change", async ({ page }) => {
    const firstPage = linesPage(page, null, 0)
    await page.goto("/lines")
    await firstPage
    await expect(page.locator("table tbody tr").first()).toBeVisible()

    const filtered = linesPage(page, "SINGTEL", 0)
    await page.getByLabel("Brand").click()
    await page.getByRole("option", { name: "Singtel", exact: true }).click()
    const data = await (await filtered).json()
    expect(data.hasNext).toBeTruthy()
    await expect(page.locator("table tbody tr")).toHaveCount(data.content.length)

    const nextPage = linesPage(page, "SINGTEL", 1)
    await scrollListToBottom(page)
    await nextPage
    await expect(page.locator("table tbody tr")).toHaveCount(data.content.length * 2)
  })

  test("device model search does not match digits inside IMEIs", async ({ page }) => {
    const response = page.waitForResponse(
      (r) => new URL(r.url()).pathname === "/api/devices" && r.ok()
    )
    await page.goto("/devices")
    const devices = (await (await response).json()) as {
      manufacturer: string
      model: string
      customer_name: string | null
    }[]
    const withDigits = devices.find((d) => /\d/.test(d.model))
    expect(withDigits, "a seeded device model with a number in it").toBeTruthy()
    const query = withDigits!.model
    const lower = query.toLowerCase()
    const expected = devices.filter(
      (d) =>
        `${d.manufacturer} ${d.model}`.toLowerCase().includes(lower) ||
        (d.customer_name ?? "").toLowerCase().includes(lower)
    ).length

    await page.getByLabel("Search").fill(query)
    await expect(page.locator("table tbody tr")).toHaveCount(expected)
  })
})
