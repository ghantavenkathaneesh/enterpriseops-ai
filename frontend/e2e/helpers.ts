import { expect, type Page } from '@playwright/test';

export const DEMO_PASSWORD = process.env.DEMO_PASSWORD ?? '';

export async function login(page: Page, email: string) {
  if (!DEMO_PASSWORD) throw new Error('Set DEMO_PASSWORD (same value the backend was seeded with)');
  await page.goto('/login');
  await page.getByLabel('Email').fill(email);
  await page.getByLabel('Password').fill(DEMO_PASSWORD);
  await page.getByRole('button', { name: 'Sign in' }).click();
  await expect(page.getByRole('button', { name: 'Log out' })).toBeVisible();
}
