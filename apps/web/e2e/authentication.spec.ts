
import { expect, test } from '@playwright/test';

import { login } from './support/auth';

test.describe('Authentication and workspace access', () => {
  test('redirects unauthenticated users away from protected routes', async ({
    page,
  }) => {
    await page.goto('/app');

    await expect(page).toHaveURL(/\/login$/);
    await expect(
      page.getByRole('button', { name: 'Sign in' }),
    ).toBeVisible();
  });

  test('allows a user to sign in and access the workspace', async ({
    page,
  }) => {
    await login(page);

    await expect(
      page.getByRole('button', { name: 'Sign out' }),
    ).toBeVisible();
  });

  test('signs out and prevents access to protected routes', async ({
    page,
  }) => {
    await login(page);

    await page.getByRole('button', { name: 'Sign out' }).click();

    await expect(page).toHaveURL('/');

    await page.goto('/app');

    await expect(page).toHaveURL(/\/login$/);
  });
});
