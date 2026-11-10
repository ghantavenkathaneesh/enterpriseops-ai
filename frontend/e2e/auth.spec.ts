import { expect, test } from '@playwright/test';
import { login } from './helpers';

test('unauthenticated users are redirected to login', async ({ page }) => {
  await page.goto('/tickets');
  await expect(page).toHaveURL(/\/login/);
});

test('wrong password shows an error', async ({ page }) => {
  await page.goto('/login');
  await page.getByLabel('Email').fill('admin@enterpriseops.local');
  await page.getByLabel('Password').fill('definitely-wrong');
  await page.getByRole('button', { name: 'Sign in' }).click();
  await expect(page.getByRole('alert')).toContainText('Invalid email or password');
});

test('admin sees the overview and can log out', async ({ page }) => {
  await login(page, 'admin@enterpriseops.local');
  await expect(page.getByRole('heading', { name: 'Overview' })).toBeVisible();
  await page.getByRole('button', { name: 'Log out' }).click();
  await expect(page).toHaveURL(/\/login/);
});

test('employee navigation hides staff-only pages', async ({ page }) => {
  await login(page, 'employee@enterpriseops.local');
  await expect(page.getByRole('link', { name: 'Incidents' })).toHaveCount(0);
  await expect(page.getByRole('link', { name: 'Users' })).toHaveCount(0);
  await expect(page.getByRole('link', { name: 'Tickets' })).toBeVisible();
});
