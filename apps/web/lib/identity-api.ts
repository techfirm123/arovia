const API_BASE_URL = (
  process.env.NEXT_PUBLIC_API_BASE_URL ?? 'http://localhost:8080'
).replace(/\/+$/, '');

export type IdentityUser = {
  id: string;
  displayName: string;
  email: string;
  emailVerified: boolean;
};

type CsrfResponse = {
  token?: string;
  headerName?: string;
};

export class IdentityApiError extends Error {
  readonly status: number;

  constructor(status: number, message: string) {
    super(message);
    this.name = 'IdentityApiError';
    this.status = status;
  }
}

async function readErrorMessage(
  response: Response,
  fallback: string,
): Promise<string> {
  try {
    const body: unknown = await response.json();

    if (
      body !== null &&
      typeof body === 'object' &&
      'message' in body &&
      typeof body.message === 'string'
    ) {
      return body.message;
    }
  } catch {
    // Some API responses intentionally have no JSON body.
  }

  return fallback;
}

async function getCsrfToken(): Promise<{
  token: string;
  headerName: string;
}> {
  const response = await fetch(
    `${API_BASE_URL}/api/v1/identity/csrf`,
    {
      method: 'GET',
      credentials: 'include',
      cache: 'no-store',
    },
  );

  if (!response.ok) {
    throw new IdentityApiError(
      response.status,
      'Unable to initialize a secure request. Please try again.',
    );
  }

  const data = (await response.json()) as CsrfResponse;

  if (!data.token) {
    throw new IdentityApiError(
      response.status,
      'The security token could not be obtained. Please refresh the page.',
    );
  }

  return {
    token: data.token,
    headerName: data.headerName || 'X-CSRF-TOKEN',
  };
}

async function postJson(
  path: string,
  payload: Record<string, string>,
): Promise<Response> {
  const csrf = await getCsrfToken();

  const response = await fetch(`${API_BASE_URL}${path}`, {
    method: 'POST',
    credentials: 'include',
    cache: 'no-store',
    headers: {
      'Content-Type': 'application/json',
      [csrf.headerName]: csrf.token,
    },
    body: JSON.stringify(payload),
  });

  if (!response.ok) {
    const message = await readErrorMessage(
      response,
      'The request could not be completed. Please try again.',
    );

    throw new IdentityApiError(response.status, message);
  }

  return response;
}

export async function loginIdentity(
  email: string,
  password: string,
): Promise<IdentityUser> {
  const response = await postJson(
    '/api/v1/identity/sessions',
    { email, password },
  );

  const data = (await response.json()) as {
    user: IdentityUser;
  };

  return data.user;
}

export async function fetchCurrentIdentity(): Promise<IdentityUser> {
  const response = await fetch(
    `${API_BASE_URL}/api/v1/identity/me`,
    {
      method: 'GET',
      credentials: 'include',
      cache: 'no-store',
    },
  );

  if (!response.ok) {
    const message = await readErrorMessage(
      response,
      'Your session could not be verified.',
    );

    throw new IdentityApiError(response.status, message);
  }

  return (await response.json()) as IdentityUser;
}

export async function logoutCurrentSession(): Promise<void> {
  const csrf = await getCsrfToken();

  const response = await fetch(
    `${API_BASE_URL}/api/v1/identity/sessions/current`,
    {
      method: 'DELETE',
      credentials: 'include',
      cache: 'no-store',
      headers: {
        [csrf.headerName]: csrf.token,
      },
    },
  );

  if (!response.ok) {
    throw new IdentityApiError(
      response.status,
      'Unable to sign out. Please try again.',
    );
  }
}

export async function requestPasswordReset(
  email: string,
): Promise<void> {
  await postJson(
    '/api/v1/identity/password-resets',
    { email },
  );
}

export async function confirmPasswordReset(
  token: string,
  newPassword: string,
): Promise<void> {
  await postJson(
    '/api/v1/identity/password-resets/confirm',
    {
      token,
      newPassword,
    },
  );
}