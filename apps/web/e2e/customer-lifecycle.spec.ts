
import { expect, test } from '@playwright/test';

import { login, selectOrganization } from './support/auth';

test.describe('Customer lifecycle', () => {
  test('creates, edits and archives a customer', async ({ page }) => {
    const customerName = `QA Customer ${Date.now()}`;
    const updatedCustomerName = `${customerName} Updated`;

    await login(page);
    await selectOrganization(page);

    await page.goto('/app/customers');

    await expect(
      page.getByRole('heading', { name: 'Customers.' }),
    ).toBeVisible();

    await page.getByRole('button', { name: 'New customer' }).click();

    await expect(
      page.getByRole('heading', { name: 'New customer' }),
    ).toBeVisible();

    await page.getByPlaceholder('Customer name').fill(customerName);

    const createResponsePromise = page.waitForResponse((response) => {
      return (
        response.request().method() === 'POST' &&
        new URL(response.url()).pathname === '/api/v1/customers'
      );
    });

    await page.getByRole('button', { name: 'Create customer' }).click();

    const createResponse = await createResponsePromise;

    if (!createResponse.ok()) {
      const responseBody = await createResponse.text();

      throw new Error(
        `Customer creation failed with HTTP ${createResponse.status()}: ${responseBody}`,
      );
    }

    const customerRow = page.getByRole('button', {
      name: new RegExp(customerName),
    });

    await expect(customerRow).toBeVisible();

    await customerRow.click();

    await expect(
      page.getByRole('heading', { name: customerName }),
    ).toBeVisible();

    await page.getByRole('button', { name: 'Edit' }).click();

    await expect(
      page.getByRole('heading', { name: 'Edit customer' }),
    ).toBeVisible();

    await page.getByPlaceholder('Customer name').fill(updatedCustomerName);

    const updateResponsePromise = page.waitForResponse((response) => {
      return (
        response.request().method() === 'PUT' &&
        new URL(response.url()).pathname.startsWith('/api/v1/customers/')
      );
    });

    await page.getByRole('button', { name: 'Save changes' }).click();

    const updateResponse = await updateResponsePromise;

    if (!updateResponse.ok()) {
      const responseBody = await updateResponse.text();

      throw new Error(
        `Customer update failed with HTTP ${updateResponse.status()}: ${responseBody}`,
      );
    }

    const updatedCustomerRow = page.getByRole('button', {
      name: new RegExp(updatedCustomerName),
    });

    await expect(updatedCustomerRow).toBeVisible();
    await updatedCustomerRow.click();

    await expect(
      page.getByRole('heading', { name: updatedCustomerName }),
    ).toBeVisible();

    await page.getByRole('button', { name: 'Archive' }).click();

    await expect(
      page.getByRole('alertdialog'),
    ).toBeVisible();

    const archiveResponsePromise = page.waitForResponse((response) => {
      return (
        response.request().method() === 'POST' &&
        new URL(response.url()).pathname.endsWith('/archive')
      );
    });

    await page
      .getByRole('alertdialog')
      .getByRole('button', { name: 'Archive customer' })
      .click();

    const archiveResponse = await archiveResponsePromise;

    if (!archiveResponse.ok()) {
      const responseBody = await archiveResponse.text();

      throw new Error(
        `Customer archive failed with HTTP ${archiveResponse.status()}: ${responseBody}`,
      );
    }

    await expect(updatedCustomerRow).toHaveCount(0);
  });
});
