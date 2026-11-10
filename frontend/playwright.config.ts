import { defineConfig } from '@playwright/test';

// Needs the full stack running (docker compose up --build) with SEED_DEMO=true. DEMO_PASSWORD must be exported for the tests.
export default defineConfig({
  testDir: './e2e',
  timeout: 30_000,
  use: { baseURL: process.env.E2E_BASE_URL ?? 'http://localhost:5173', trace: 'retain-on-failure' },
});
