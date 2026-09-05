import { defineConfig } from '@playwright/test'
export default defineConfig({
  testDir: '.',
  testMatch: 'workbench.spec.ts',
  timeout: 30000,
  workers: 1,
  outputDir: '../test-results',
  use: {
    baseURL: 'http://127.0.0.1:5186',
    channel: process.env.PLAYWRIGHT_CHANNEL || undefined,
    viewport: { width: 1440, height: 1050 },
    screenshot: 'only-on-failure',
    trace: 'retain-on-failure',
  },
  webServer: {
    command: 'npm run dev -- --host 127.0.0.1 --port 5186 --strictPort',
    url: 'http://127.0.0.1:5186',
    reuseExistingServer: !process.env.CI,
    cwd: '..',
  },
})
