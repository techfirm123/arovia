
import { spawnSync } from 'node:child_process';

import { request, type APIRequestContext } from '@playwright/test';

const API_BASE_URL =
  process.env.E2E_API_BASE_URL ?? 'http://localhost:8080';

const WEB_ORIGIN =
  process.env.E2E_BASE_URL ?? 'http://localhost:3000';

const EMAIL = process.env.E2E_USER_EMAIL;
const PASSWORD = process.env.E2E_USER_PASSWORD;

type CsrfResponse = {
  headerName: string;
  token: string;
};

async function requireSuccess(
  response: Awaited<ReturnType<APIRequestContext['get']>>,
  operation: string,
): Promise<void> {
  if (response.ok()) {
    return;
  }

  throw new Error(
    `${operation} failed with HTTP ${response.status()}: ${await response.text()}`,
  );
}

async function getCsrf(
  api: APIRequestContext,
): Promise<{ token: string; headerName: string }> {
  const response = await api.get('/api/v1/identity/csrf');
  await requireSuccess(response, 'CSRF initialization');

  const body = (await response.json()) as CsrfResponse;

  if (!body.token) {
    throw new Error('The API did not return a CSRF token.');
  }

 if (!body.token || !body.headerName) {
   throw new Error('The API did not return a complete CSRF token response.');
 }

 return {
   token: body.token,
   headerName: body.headerName,
 };
}

function activateRegisteredAccount(email: string): void {
  const result = spawnSync(
    'psql',
    [
      '-v',
      'ON_ERROR_STOP=1',
      '-tA',
      '-v',
      `e2e_email=${email}`,
      '-c',
      `
        UPDATE user_account
        SET
          status = 'ACTIVE',
          email_verified_at = CURRENT_TIMESTAMP,
          updated_at = CURRENT_TIMESTAMP
        WHERE normalized_email = :'e2e_email'
        RETURNING id;
      `,
    ],
    {
      encoding: 'utf8',
      env: process.env,
    },
  );

  if (result.error) {
    throw result.error;
  }

  if (result.status !== 0) {
    throw new Error(
      `Unable to activate the E2E account: ${result.stderr}`,
    );
  }

  if (!result.stdout.trim()) {
    throw new Error(
      `No registered account was found for ${email}.`,
    );
  }
}

async function bootstrap(): Promise<void> {
  if (process.env.E2E_BOOTSTRAP !== 'true') {
    return;
  }

  if (!EMAIL || !PASSWORD) {
    throw new Error(
      'E2E_USER_EMAIL and E2E_USER_PASSWORD are required when E2E_BOOTSTRAP=true.',
    );
  }

  const api = await request.newContext({
    baseURL: API_BASE_URL,
    extraHTTPHeaders: {
      Origin: WEB_ORIGIN,
    },
  });

  try {
    // Register through the real API so the production password encoder
    // creates the password hash. Email delivery is disabled in CI.
    const registrationCsrf = await getCsrf(api);

    const registration = await api.post(
      '/api/v1/identity/registrations',
      {
        headers: {
          [registrationCsrf.headerName]: registrationCsrf.token,
        },
        data: {
          displayName: 'Arovia E2E Test User',
          email: EMAIL,
          password: PASSWORD,
        },
      },
    );

    await requireSuccess(registration, 'E2E account registration');

    // Email verification is deliberately bypassed only in the isolated
    // E2E database. The real registration and password hashing remain.
    activateRegisteredAccount(EMAIL);

    // Authenticate through the real login endpoint.
    const loginCsrf = await getCsrf(api);

    const login = await api.post('/api/v1/identity/sessions', {
      headers: {
        [loginCsrf.headerName]: loginCsrf.token,
      },
      data: {
        email: EMAIL,
        password: PASSWORD,
      },
    });

    await requireSuccess(login, 'E2E account login');

    // Authentication rotates the CSRF token.
    const organizationCsrf = await getCsrf(api);

    const createOrganization = await api.post(
      '/api/v1/organizations',
      {
        headers: {
          [organizationCsrf.headerName]: organizationCsrf.token,
        },
        data: {
          name: `Arovia E2E ${process.env.GITHUB_RUN_ID ?? Date.now()}`,
          profile: {
            displayName: 'Arovia E2E Workspace',
            countryCode: 'IN',
            defaultCurrency: 'INR',
            timezone: 'Asia/Kolkata',
          },
          initialLocation: {
            name: 'Main Branch',
            locationType: 'BUSINESS',
            countryCode: 'IN',
            timezone: 'Asia/Kolkata',
          },
        },
      },
    );

    await requireSuccess(
      createOrganization,
      'E2E organization creation',
    );

    const organizationsResponse = await api.get(
      '/api/v1/organizations',
    );

    await requireSuccess(
      organizationsResponse,
      'E2E organization lookup',
    );

    const organizations = (await organizationsResponse.json()) as Array<{
      organizationId: string;
    }>;

    const organization = organizations[0];

    if (!organization?.organizationId) {
      throw new Error(
        'The E2E account has no active organization after provisioning.',
      );
    }

    const selectionCsrf = await getCsrf(api);

    const selection = await api.put(
      '/api/v1/organizations/active',
      {
        headers: {
          [selectionCsrf.headerName]: selectionCsrf.token,
        },
        data: {
          organizationId: organization.organizationId,
        },
      },
    );

    await requireSuccess(
      selection,
      'E2E active organization selection',
    );
  } finally {
    await api.dispose();
  }
}

export default async function globalSetup(): Promise<void> {
  await bootstrap();
}
