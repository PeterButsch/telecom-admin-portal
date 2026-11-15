import { test, expect, type APIRequestContext } from "@playwright/test"
import { anonymousApi, backendUrl, signInApi } from "./support"

test.describe("API Health", () => {
  let api: APIRequestContext

  test.beforeAll(async () => {
    api = await signInApi()
  })

  test.afterAll(async () => {
    await api.dispose()
  })

  test("backend health endpoint should respond", async ({ request }) => {
    const response = await request.get(`${backendUrl}/api/health`)
    expect(response.ok()).toBeTruthy()
  })

  test("data endpoints require a signed-in user", async () => {
    const anonymous = await anonymousApi()
    const response = await anonymous.get("/api/customers")
    expect(response.status()).toBe(401)
    await anonymous.dispose()
  })

  for (const path of ["/api/customers", "/api/lines", "/api/usage", "/api/cases"]) {
    test(`${path} should return a page`, async () => {
      const response = await api.get(`${path}?size=5`)
      expect(response.ok()).toBeTruthy()
      const data = await response.json()
      expect(Array.isArray(data.content)).toBeTruthy()
      expect(data.content.length).toBeGreaterThan(0)
    })
  }

  for (const path of ["/api/plans", "/api/devices"]) {
    test(`${path} should return a list`, async () => {
      const response = await api.get(path)
      expect(response.ok()).toBeTruthy()
      expect(Array.isArray(await response.json())).toBeTruthy()
    })
  }

  test("dashboard stats API should respond", async () => {
    const response = await api.get("/api/dashboard/stats")
    expect(response.ok()).toBeTruthy()
  })
})
