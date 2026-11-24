
import { expect, type Page } from '@playwright/test';

export const testCredentials = {
  email: process.env.E2E_USER_EMAIL ?? '',
  password: process.env.E2E_USER_PASSWORD ?? '',
};

export async function login(page: Page): Promise<void> {
  if (!testCredentials.email || !testCredentials.password) {
    throw new Error(
      'Set E2E_USER_EMAIL and E2E_USER_PASSWORD before running E2E tests.',
    );
  }

  await page.goto('/login');

  await page.getByLabel('Email address').fill(testCredentials.email);
  await page.getByLabel('Password').fill(testCredentials.password);
  await page.getByRole('button', { name: 'Sign in' }).click();

  await expect(page).toHaveURL(/\/app(?:\/|$)/);
}

export async function selectOrganization(
  page: Page,
  organizationName?: string,
): Promise<void> {
  const selector = page.locator('#organization-selector');

  await expect(selector).toBeVisible();

  if (organizationName) {
    await selector.selectOption({
      label: organizationName,
    });
  } else {
    const options = await selector.locator('option').allTextContents();
    const organization = options
      .map((option) => option.trim())
      .find((option) => option && option !== 'Select organization');

    if (!organization) {
      throw new Error('No organization is available for this test account.');
    }

    await selector.selectOption({ label: organization });
  }

  await expect(selector).not.toHaveValue('');
}
