import { expect, test } from '@playwright/test';
import { login } from './helpers';

test('agent can open the incidents list and view an incident', async ({ page }) => {
  await login(page, 'agent1@enterpriseops.local');
  await page.getByRole('link', { name: 'Incidents' }).click();
  await expect(page.getByRole('heading', { name: 'Incidents' })).toBeVisible();
  await page.getByRole('button', { name: 'New incident' }).click();
  const title = `E2E outage ${Date.now()}`;
  await page.getByLabel('Title').fill(title);
  await page.getByLabel('Description').fill('Synthetic outage for UI test');
  await page.getByRole('button', { name: 'Create incident' }).click();
  await page.getByText(title).click();
  await expect(page.getByRole('dialog')).toContainText(title);
});
