
import { expect, test } from '@playwright/test';

import { login, selectOrganization } from './support/auth';

test.describe('Organization context', () => {
  test('selects an organization and retains the active workspace context', async ({
    page,
  }) => {
    await login(page);

    await page.goto('/app');

    const selector = page.locator('#organization-selector');
    await expect(selector).toBeVisible();

    const options = await selector.locator('option').allTextContents();
    const organizationNames = options
      .map((name) => name.trim())
      .filter((name) => name && name !== 'Select organization');

    if (organizationNames.length === 0) {
      await page.goto('/app/organizations/new');

      await page.getByLabel('Business name *').fill('QA Automation Business');
      await page.getByRole('button', { name: /create organization/i }).click();

      await expect(page).toHaveURL(/\/app(?:\/|$)/);
    } else {
      await selectOrganization(page, organizationNames[0]);
    }

    await expect(selector).not.toHaveValue('');

    const selectedOrganizationId = await selector.inputValue();

    await page.reload();

    await expect(selector).toHaveValue(selectedOrganizationId);
  });
});
