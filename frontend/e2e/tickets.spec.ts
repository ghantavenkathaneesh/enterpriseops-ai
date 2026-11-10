import { expect, test } from '@playwright/test';
import { login } from './helpers';

test('employee creates a ticket and agent assigns it', async ({ browser }) => {
  const title = `E2E printer jam ${Date.now()}`;

  const employee = await (await browser.newContext()).newPage();
  await login(employee, 'employee@enterpriseops.local');
  await employee.goto('/tickets');
  await employee.getByRole('button', { name: 'New ticket' }).click();
  await employee.getByLabel('Title').fill(title);
  await employee.getByLabel('Description').fill('Paper jam on the third floor printer');
  await employee.getByLabel('Category').selectOption('hardware');
  await employee.getByRole('button', { name: 'Create ticket' }).click();
  await expect(employee.getByText(title)).toBeVisible();

  const agent = await (await browser.newContext()).newPage();
  await login(agent, 'agent1@enterpriseops.local');
  await agent.goto('/tickets');
  await agent.getByPlaceholder('Search title or description').fill(title);
  await agent.getByText(title).click();
  await agent.getByLabel('Assign to').selectOption({ label: 'Demo Agent One' });
  await agent.getByLabel('Change status').selectOption('IN_PROGRESS');
  await expect(agent.getByText('IN PROGRESS').first()).toBeVisible();
});
