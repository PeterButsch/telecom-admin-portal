import { test as setup } from "@playwright/test"
import { ADMIN_STATE, signInUi } from "./support"

setup("sign in as administrator", async ({ page }) => {
  await signInUi(page, "admin")
  await page.context().storageState({ path: ADMIN_STATE })
})
