const API_BASE_URL = (
  process.env.NEXT_PUBLIC_API_BASE_URL ?? 'http://localhost:8080'
).replace(/\/+$/, '');

const CUSTOMER_API = '/api/v1/customers';
const CSRF_API = '/api/v1/identity/csrf';
const DEFAULT_CSRF_HEADER_NAME = 'X-CSRF-TOKEN';

export type CustomerType = 'INDIVIDUAL' | 'BUSINESS';
export type CustomerStatus = 'ACTIVE' | 'ARCHIVED';

export type Customer = {
  id: string;
  customerNumber: string;
  customerType: CustomerType;
  displayName: string;
  legalName: string | null;
  email: string | null;
  phone: string | null;
  notes: string | null;
  status: CustomerStatus;
  createdAt: string;
  updatedAt: string;
};

export type CustomerPayload = {
  customerType: CustomerType;
  displayName: string;
  legalName: string | null;
  email: string | null;
  phone: string | null;
  notes: string | null;
};

export type CustomerPage = {
  items: Customer[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
};

type CsrfResponse = {
  token?: string;
  headerName?: string;
};

export class CustomerApiError extends Error {
  readonly status: number;

  constructor(status: number, message: string) {
    super(message);
    this.name = 'CustomerApiError';
    this.status = status;
  }
}

async function readErrorMessage(
  response: Response,
  fallback: string,
): Promise<string> {
  try {
    const body: unknown = await response.json();

    if (body !== null && typeof body === 'object') {
      if ('detail' in body && typeof body.detail === 'string') {
        return body.detail;
      }

      if ('message' in body && typeof body.message === 'string') {
        return body.message;
      }
    }
  } catch {
    // The API may return an empty response.
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
    throw new CustomerApiError(
      response.status,
      'Unable to initialize a secure request.',
    );
  }

  const data = (await response.json()) as CsrfResponse;

  if (!data.token) {
    throw new CustomerApiError(
      response.status,
      'Unable to obtain the security token.',
    );
  }

  return {
    token: data.token,
    headerName: data.headerName || DEFAULT_CSRF_HEADER_NAME,
  };
}

async function secureRequest(
  url: string,
  method: 'POST' | 'PUT',
  body?: unknown,
): Promise<Response> {
  const csrf = await getCsrfToken();

  return fetch(url, {
    method,
    credentials: 'include',
    cache: 'no-store',
    headers: {
      'Content-Type': 'application/json',
      [csrf.headerName]: csrf.token,
    },
    body: body === undefined ? undefined : JSON.stringify(body),
  });
}

export async function fetchCustomers(
  search: string,
  page: number,
  size = 20,
): Promise<CustomerPage> {
  const params = new URLSearchParams({
    page: String(page),
    size: String(size),
  });

  if (search.trim()) {
    params.set('search', search.trim());
  }

  const response = await fetch(
    `${API_BASE_URL}${CUSTOMER_API}?${params.toString()}`,
    {
      method: 'GET',
      credentials: 'include',
      cache: 'no-store',
    },
  );

  if (!response.ok) {
    throw new CustomerApiError(
      response.status,
      await readErrorMessage(
        response,
        'Unable to load customers.',
      ),
    );
  }

  return (await response.json()) as CustomerPage;
}

export async function fetchCustomer(
  customerId: string,
): Promise<Customer> {
  const response = await fetch(
    `${API_BASE_URL}${CUSTOMER_API}/${encodeURIComponent(customerId)}`,
    {
      method: 'GET',
      credentials: 'include',
      cache: 'no-store',
    },
  );

  if (!response.ok) {
    throw new CustomerApiError(
      response.status,
      await readErrorMessage(
        response,
        'Unable to load this customer.',
      ),
    );
  }

  return (await response.json()) as Customer;
}

export async function createCustomer(
  payload: CustomerPayload,
): Promise<Customer> {
  const response = await secureRequest(
    `${API_BASE_URL}${CUSTOMER_API}`,
    'POST',
    payload,
  );

  if (!response.ok) {
    throw new CustomerApiError(
      response.status,
      await readErrorMessage(
        response,
        'Unable to create customer.',
      ),
    );
  }

  return (await response.json()) as Customer;
}

export async function updateCustomer(
  customerId: string,
  payload: CustomerPayload,
): Promise<Customer> {
  const response = await secureRequest(
    `${API_BASE_URL}${CUSTOMER_API}/${encodeURIComponent(customerId)}`,
    'PUT',
    payload,
  );

  if (!response.ok) {
    throw new CustomerApiError(
      response.status,
      await readErrorMessage(
        response,
        'Unable to update customer.',
      ),
    );
  }

  return (await response.json()) as Customer;
}

export async function archiveCustomer(
  customerId: string,
): Promise<void> {
  const csrf = await getCsrfToken();

  const response = await fetch(
    `${API_BASE_URL}${CUSTOMER_API}/${encodeURIComponent(customerId)}/archive`,
    {
      method: 'POST',
      credentials: 'include',
      cache: 'no-store',
      headers: {
        [csrf.headerName]: csrf.token,
      },
    },
  );

  if (!response.ok) {
    throw new CustomerApiError(
      response.status,
      await readErrorMessage(
        response,
        'Unable to archive customer.',
      ),
    );
  }
}