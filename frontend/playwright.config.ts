import { defineConfig, devices } from '@playwright/test';
export default defineConfig({
  testDir: './e2e',
  workers: 1,
  use: {
    baseURL: process.env['RECICLO_E2E_URL'] ?? 'http://localhost:4200',
    channel: 'chrome',
    trace: 'retain-on-failure',
    timezoneId: 'America/Lima',
  },
  projects: [
    { name: 'escritorio', use: { ...devices['Desktop Chrome'], channel: 'chrome' } },
    { name: 'movil', use: { ...devices['Pixel 7'], channel: 'chrome' } },
  ],
  reporter: [['list']],
});
