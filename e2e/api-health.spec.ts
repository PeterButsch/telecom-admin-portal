import { test, expect } from "@playwright/test"

const backendUrl = process.env.BACKEND_URL ?? "http://localhost:8080"

test.describe("API Health", () => {
  test("backend health endpoint should respond", async ({ request }) => {
    const response = await request.get(`${backendUrl}/api/health`)
    expect(response.ok()).toBeTruthy()
  })

  for (const path of ["/api/customers", "/api/lines", "/api/usage", "/api/cases"]) {
    test(`${path} should return a page`, async ({ request }) => {
      const response = await request.get(`${backendUrl}${path}?size=5`)
      expect(response.ok()).toBeTruthy()
      const data = await response.json()
      expect(Array.isArray(data.content)).toBeTruthy()
      expect(data.content.length).toBeGreaterThan(0)
    })
  }

  for (const path of ["/api/plans", "/api/devices"]) {
    test(`${path} should return a list`, async ({ request }) => {
      const response = await request.get(`${backendUrl}${path}`)
      expect(response.ok()).toBeTruthy()
      expect(Array.isArray(await response.json())).toBeTruthy()
    })
  }

  test("dashboard stats API should respond", async ({ request }) => {
    const response = await request.get(`${backendUrl}/api/dashboard/stats`)
    expect(response.ok()).toBeTruthy()
  })
})
