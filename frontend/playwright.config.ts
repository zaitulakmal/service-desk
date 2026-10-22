import { defineConfig, devices } from '@playwright/test'

/**
 * End-to-end tests drive a real browser through Keycloak login and the whole stack.
 *
 * Local dev: docker compose up -d, then the api and notification-service
 * (./mvnw spring-boot:run). Playwright starts the web app itself if it is not running.
 *
 * Kubernetes: E2E_BASE_URL=http://servicedesk.localhost npm run e2e
 */
const baseURL = process.env.E2E_BASE_URL ?? 'http://localhost:5173'

export default defineConfig({
  testDir: './e2e',
  timeout: 60_000,
  fullyParallel: false,
  workers: 1,
  reporter: [['list']],
  use: {
    baseURL,
    screenshot: 'only-on-failure',
    trace: 'retain-on-failure',
  },
  projects: [{ name: 'chromium', use: { ...devices['Desktop Chrome'] } }],
  webServer: process.env.E2E_BASE_URL
    ? undefined
    : { command: 'npm run dev', url: 'http://localhost:5173', reuseExistingServer: true },
})
