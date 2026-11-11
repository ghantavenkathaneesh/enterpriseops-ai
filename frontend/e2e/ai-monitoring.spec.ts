import { expect, test } from '@playwright/test';
import { login } from './helpers';

test('AI classification and live monitoring work against the running services', async ({ page }) => {
  await login(page, 'admin@enterpriseops.local');

  await page.getByRole('link', { name: 'AI Insights' }).click();
  await expect(page.getByText('AI service online')).toBeVisible();
  await page.getByLabel('Issue description').fill('VPN keeps dropping since morning');
  await page.getByRole('button', { name: 'Classify and suggest actions' }).click();
  await expect(page.getByText(/Category:/)).toBeVisible();
  await expect(page.getByRole('listitem').first()).toBeVisible();

  await page.getByRole('link', { name: 'Monitoring' }).click();
  await expect(page.getByRole('heading', { name: 'Monitoring' })).toBeVisible();
  await expect(page.getByText('Live', { exact: true })).toBeVisible({ timeout: 20_000 });
  await expect(page.getByText(/CPU \d+(?:\.\d+)?%/).first()).toBeVisible({ timeout: 20_000 });
});
