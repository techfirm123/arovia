
const API_BASE_URL = (
  process.env.NEXT_PUBLIC_API_BASE_URL ?? 'http://localhost:8080'
).replace(/\/+$/, '');

const ORGANIZATION_API = '/api/v1/organizations';
const CSRF_API = '/api/v1/identity/csrf';

const DEFAULT_CSRF_HEADER_NAME = 'X-CSRF-TOKEN';

export type OrganizationMembership = {
  organizationId: string;
  organizationName: string;
  organizationStatus: string;
  displayName: string | null;
  membershipId: string;
  membershipStatus: string;
  joinedAt: string;
  roles: string[];
};

export type CreateOrganizationPayload = {
  name: string;
  profile: {
    displayName: string;
    legalName: string;
    countryCode: string;
    defaultCurrency: string;
    timezone: string;
    websiteUrl?: string;
    contactEmail?: string;
    contactPhone?: string;
  };
  initialLocation?: {
    name: string;
    locationCode?: string;
    locationType: string;
    addressLine1?: string;
    addressLine2?: string;
    city?: string;
    stateOrRegion?: string;
    postalCode?: string;
    countryCode: string;
    timezone: string;
  } | null;
};

export type CreatedOrganization = {
  id: string;
  name: string;
  status: string;
  membershipId: string;
  membershipStatus: string;
  role: string;
  createdAt: string;
};

type CsrfResponse = {
  token?: string;
  headerName?: string;
};

export class OrganizationApiError extends Error {
  readonly status: number;

  constructor(status: number, message: string) {
    super(message);
    this.name = 'OrganizationApiError';
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
    // The API may intentionally return an empty response.
  }

  return fallback;
}

async function getCsrfToken(): Promise<{
  token: string;
  headerName: string;
}> {
  const response = await fetch(`${API_BASE_URL}${CSRF_API}`, {
    method: 'GET',
    credentials: 'include',
    cache: 'no-store',
  });

  if (!response.ok) {
    throw new OrganizationApiError(
      response.status,
      'Unable to initialize a secure request.',
    );
  }

  const data = (await response.json()) as CsrfResponse;

  if (!data.token) {
    throw new OrganizationApiError(
      response.status,
      'Unable to obtain the security token.',
    );
  }

  return {
    token: data.token,
    headerName: data.headerName || DEFAULT_CSRF_HEADER_NAME,
  };
}

export async function fetchOrganizations(): Promise<
  OrganizationMembership[]
> {
  const response = await fetch(`${API_BASE_URL}${ORGANIZATION_API}`, {
    method: 'GET',
    credentials: 'include',
    cache: 'no-store',
  });

  if (!response.ok) {
    throw new OrganizationApiError(
      response.status,
      await readErrorMessage(
        response,
        'Unable to retrieve your organizations.',
      ),
    );
  }

  return (await response.json()) as OrganizationMembership[];
}

export async function fetchActiveOrganization(): Promise<
  OrganizationMembership | null
> {
  const response = await fetch(
    `${API_BASE_URL}${ORGANIZATION_API}/active`,
    {
      method: 'GET',
      credentials: 'include',
      cache: 'no-store',
    },
  );

  if (response.status === 204) {
    return null;
  }

  if (!response.ok) {
    throw new OrganizationApiError(
      response.status,
      await readErrorMessage(
        response,
        'Unable to retrieve the active organization.',
      ),
    );
  }

  return (await response.json()) as OrganizationMembership;
}

export async function selectActiveOrganization(
  organizationId: string,
): Promise<OrganizationMembership> {
  const csrf = await getCsrfToken();

  const response = await fetch(
    `${API_BASE_URL}${ORGANIZATION_API}/active`,
    {
      method: 'PUT',
      credentials: 'include',
      cache: 'no-store',
      headers: {
        'Content-Type': 'application/json',
        [csrf.headerName]: csrf.token,
      },
      body: JSON.stringify({ organizationId }),
    },
  );

  if (!response.ok) {
    throw new OrganizationApiError(
      response.status,
      await readErrorMessage(
        response,
        'Unable to select the organization.',
      ),
    );
  }

  return (await response.json()) as OrganizationMembership;
}

export async function clearActiveOrganization(): Promise<void> {
  const csrf = await getCsrfToken();

  const response = await fetch(
    `${API_BASE_URL}${ORGANIZATION_API}/active`,
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
    throw new OrganizationApiError(
      response.status,
      await readErrorMessage(
        response,
        'Unable to clear the active organization.',
      ),
    );
  }
}

export async function createOrganization(
  payload: CreateOrganizationPayload,
): Promise<CreatedOrganization> {
  const csrf = await getCsrfToken();

  const response = await fetch(`${API_BASE_URL}${ORGANIZATION_API}`, {
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
    throw new OrganizationApiError(
      response.status,
      await readErrorMessage(
        response,
        'Unable to create your organization.',
      ),
    );
  }

  return (await response.json()) as CreatedOrganization;
}
