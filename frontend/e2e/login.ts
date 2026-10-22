import { type Browser, type Page, expect } from '@playwright/test'

/** Demo accounts from deploy/keycloak/servicedesk-realm.json. All use the password "password". */
export type DemoUser = 'aina' | 'farah' | 'daniel' | 'siti' | 'hafiz' | 'kumar' | 'mei'

/** Opens a fresh browser session signed in as this person, through the real Keycloak login page. */
export async function signIn(browser: Browser, user: DemoUser, path = '/'): Promise<Page> {
  const context = await browser.newContext()
  const page = await context.newPage()
  await page.goto(path)
  await page.locator('#username').fill(user)
  await page.locator('#password').fill('password')
  await page.locator('#kc-login').click()
  await expect(page.getByRole('navigation', { name: 'Main' })).toBeVisible()
  return page
}
