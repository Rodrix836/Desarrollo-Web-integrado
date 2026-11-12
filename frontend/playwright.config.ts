import { defineConfig } from '@playwright/test';
export default defineConfig({
  testDir: './e2e',
  workers: 1,
  use: { baseURL: 'http://localhost:4200', channel: 'chrome', trace: 'retain-on-failure' },
  reporter: [['list']],
});
