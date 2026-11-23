import { expect, request, type APIRequestContext, type Page } from "@playwright/test"

export const backendUrl = process.env.BACKEND_URL ?? "http://localhost:8080"
export const DEMO_PASSWORD = process.env.APP_DEMO_PASSWORD ?? "Demo-Pass-2026"
/** Browser state of a signed-in administrator, written by auth.setup.ts. */
export const ADMIN_STATE = "e2e/.auth/admin.json"
/** No cookies. Signing in replaces the session it is sent with, so helpers must not reuse one. */
export const NO_SESSION = { cookies: [], origins: [] }

/** An API client with no session. */
export function anonymousApi(): Promise<APIRequestContext> {
  return request.newContext({ baseURL: backendUrl, storageState: NO_SESSION })
}

/** An API client signed in to the backend as one of the demo accounts. */
export async function signInApi(username = "admin"): Promise<APIRequestContext> {
  const api = await anonymousApi()
  const csrf = await api.get("/api/auth/csrf")
  expect(csrf.ok()).toBeTruthy()
  const { token } = (await csrf.json()) as { token: string }
  const login = await api.post("/api/auth/login", {
    data: { username, password: DEMO_PASSWORD },
    headers: { "X-XSRF-TOKEN": token },
  })
  expect(login.ok(), `sign in as ${username}`).toBeTruthy()
  return api
}

/** Signs in through the sign-in page with a username and the demo password. */
export async function signInUi(page: Page, username: string) {
  await page.goto("/login")
  await page.getByLabel("Username").fill(username)
  await page.getByLabel("Password").fill(DEMO_PASSWORD)
  await page.getByRole("button", { name: "Sign in", exact: true }).click()
  await expect(page).not.toHaveURL(/\/login/)
}
