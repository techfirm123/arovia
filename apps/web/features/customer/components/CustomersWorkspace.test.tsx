
import { beforeEach, describe, expect, it, vi } from 'vitest';
import {
  render,
  screen,
  waitFor,
} from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import {
  archiveCustomer,
  createCustomer,
  fetchCustomer,
  fetchCustomers,
  updateCustomer,
} from '@/features/customer/api/customer-api';

import type {
  Customer,
  CustomerPage,
} from '@/features/customer/api/customer-api';

import CustomersWorkspace from './CustomersWorkspace';

vi.mock('@/features/customer/api/customer-api', () => ({
  fetchCustomers: vi.fn(),
  fetchCustomer: vi.fn(),
  createCustomer: vi.fn(),
  updateCustomer: vi.fn(),
  archiveCustomer: vi.fn(),
  CustomerApiError: class CustomerApiError extends Error {
    readonly status: number;

    constructor(status: number, message: string) {
      super(message);
      this.name = 'CustomerApiError';
      this.status = status;
    }
  },
}));

const customer: Customer = {
  id: 'customer-001',
  customerNumber: 'CUS-000001',
  customerType: 'INDIVIDUAL',
  displayName: 'Ananya Rao',
  legalName: null,
  email: 'ananya@example.com',
  phone: '+919876543210',
  notes: null,
  status: 'ACTIVE',
  createdAt: '2026-11-20T10:00:00Z',
  updatedAt: '2026-11-20T10:00:00Z',
};

const updatedCustomer: Customer = {
  ...customer,
  displayName: 'Ananya Rao Updated',
  email: 'ananya.updated@example.com',
  phone: '+919999999999',
  updatedAt: '2026-11-24T10:00:00Z',
};

const populatedPage: CustomerPage = {
  items: [customer],
  page: 0,
  size: 20,
  totalElements: 1,
  totalPages: 1,
};

const updatedPage: CustomerPage = {
  ...populatedPage,
  items: [updatedCustomer],
};

const emptyPage: CustomerPage = {
  items: [],
  page: 0,
  size: 20,
  totalElements: 0,
  totalPages: 0,
};

describe('CustomersWorkspace', () => {
  beforeEach(() => {
    vi.resetAllMocks();

    vi.mocked(fetchCustomers).mockResolvedValue(populatedPage);
  });

  describe('customer directory', () => {
    it('loads and displays customers in the directory', async () => {
      render(<CustomersWorkspace />);

      expect(
        screen.getByRole('heading', { name: /customers/i }),
      ).toBeInTheDocument();

      await waitFor(() => {
        expect(fetchCustomers).toHaveBeenCalledWith('', 0);
      });

      expect(
        await screen.findByText('Ananya Rao'),
      ).toBeInTheDocument();

      expect(
        screen.getByText('CUS-000001'),
      ).toBeInTheDocument();

      expect(
        screen.getByText('ananya@example.com'),
      ).toBeInTheDocument();

      expect(
        screen.getByText('1 customers'),
      ).toBeInTheDocument();
    });

    it('displays the empty state when no customers exist', async () => {
      vi.mocked(fetchCustomers).mockResolvedValue(emptyPage);

      render(<CustomersWorkspace />);

      expect(
        await screen.findByText('No customers yet'),
      ).toBeInTheDocument();

      expect(
        screen.getByText(
          'Create your first customer to start building your directory.',
        ),
      ).toBeInTheDocument();

      expect(
        screen.getByRole('button', { name: /add customer/i }),
      ).toBeInTheDocument();
    });

    it('displays an error when loading customers fails', async () => {
      vi.mocked(fetchCustomers).mockRejectedValue(
        new Error('Unable to load customers.'),
      );

      render(<CustomersWorkspace />);

      expect(
        await screen.findByRole('alert'),
      ).toHaveTextContent('Unable to load customers.');
    });
  });

  describe('customer creation', () => {
    it('opens and closes the creation form without creating a customer', async () => {
      const user = userEvent.setup();

      render(<CustomersWorkspace />);

      await user.click(
        screen.getByRole('button', { name: /new customer/i }),
      );

      expect(
        screen.getByRole('dialog'),
      ).toBeInTheDocument();

      expect(
        screen.getByRole('heading', { name: 'New customer' }),
      ).toBeInTheDocument();

      await user.click(
        screen.getByRole('button', { name: /cancel/i }),
      );

      expect(
        screen.queryByRole('dialog'),
      ).not.toBeInTheDocument();

      expect(createCustomer).not.toHaveBeenCalled();
    });

    it('creates a customer successfully', async () => {
      const user = userEvent.setup();

      vi.mocked(createCustomer).mockResolvedValue(customer);

      render(<CustomersWorkspace />);

      await user.click(
        screen.getByRole('button', { name: /new customer/i }),
      );

      await user.type(
        screen.getByLabelText(/display name/i),
        'Ananya Rao',
      );

      await user.type(
        screen.getByLabelText(/email/i),
        'ananya@example.com',
      );

      await user.type(
        screen.getByLabelText(/phone/i),
        '+919876543210',
      );

      await user.click(
        screen.getByRole('button', { name: /create customer/i }),
      );

      await waitFor(() => {
        expect(createCustomer).toHaveBeenCalledWith({
          customerType: 'INDIVIDUAL',
          displayName: 'Ananya Rao',
          legalName: null,
          email: 'ananya@example.com',
          phone: '+919876543210',
          notes: null,
        });
      });

      await waitFor(() => {
        expect(
          screen.queryByRole('dialog'),
        ).not.toBeInTheDocument();
      });

      expect(
        screen.getAllByText('Ananya Rao'),
      ).toHaveLength(2);
    });

    it('displays an error and keeps the form open when creation fails', async () => {
      const user = userEvent.setup();

      vi.mocked(createCustomer).mockRejectedValue(
        new Error('Unable to create customer.'),
      );

      render(<CustomersWorkspace />);

      await user.click(
        screen.getByRole('button', { name: /new customer/i }),
      );

      await user.type(
        screen.getByLabelText(/display name/i),
        'Ananya Rao',
      );

      await user.click(
        screen.getByRole('button', { name: /create customer/i }),
      );

      expect(
        await screen.findByRole('alert'),
      ).toHaveTextContent('Unable to create customer.');

      expect(
        screen.getByRole('dialog'),
      ).toBeInTheDocument();

      expect(
        screen.getByLabelText(/display name/i),
      ).toHaveValue('Ananya Rao');
    });
  });

  describe('customer editing', () => {
    async function openCustomerForEditing(
      user: ReturnType<typeof userEvent.setup>,
    ) {
      vi.mocked(fetchCustomer).mockResolvedValue(customer);

      render(<CustomersWorkspace />);

      await screen.findByText('Ananya Rao');

      await user.click(
        screen.getByRole('button', { name: /ananya rao/i }),
      );

      await waitFor(() => {
        expect(fetchCustomer).toHaveBeenCalledWith(customer.id);
      });

      await user.click(
        screen.getByRole('button', { name: /^edit$/i }),
      );

      expect(
        screen.getByRole('dialog'),
      ).toBeInTheDocument();
    }

    it('opens the edit form with existing customer information', async () => {
      const user = userEvent.setup();

      await openCustomerForEditing(user);

      expect(
        screen.getByRole('heading', { name: 'Edit customer' }),
      ).toBeInTheDocument();

      expect(
        screen.getByLabelText(/display name/i),
      ).toHaveValue('Ananya Rao');

      expect(
        screen.getByLabelText(/email/i),
      ).toHaveValue('ananya@example.com');

      expect(
        screen.getByLabelText(/phone/i),
      ).toHaveValue('+919876543210');
    });

    it('updates customer information successfully', async () => {
      const user = userEvent.setup();

      vi.mocked(updateCustomer).mockResolvedValue(updatedCustomer);

      vi.mocked(fetchCustomers)
        .mockResolvedValueOnce(populatedPage)
        .mockResolvedValue(updatedPage);

      await openCustomerForEditing(user);

      await user.clear(
        screen.getByLabelText(/display name/i),
      );

      await user.type(
        screen.getByLabelText(/display name/i),
        'Ananya Rao Updated',
      );

      await user.clear(
        screen.getByLabelText(/email/i),
      );

      await user.type(
        screen.getByLabelText(/email/i),
        'ananya.updated@example.com',
      );

      await user.clear(
        screen.getByLabelText(/phone/i),
      );

      await user.type(
        screen.getByLabelText(/phone/i),
        '+919999999999',
      );

      await user.click(
        screen.getByRole('button', { name: /save changes/i }),
      );

      await waitFor(() => {
        expect(updateCustomer).toHaveBeenCalledWith(
          customer.id,
          {
            customerType: 'INDIVIDUAL',
            displayName: 'Ananya Rao Updated',
            legalName: null,
            email: 'ananya.updated@example.com',
            phone: '+919999999999',
            notes: null,
          },
        );
      });

      await waitFor(() => {
        expect(
          screen.queryByRole('dialog'),
        ).not.toBeInTheDocument();
      });

      expect(
        await screen.findByRole('heading', {
          name: 'Ananya Rao Updated',
        }),
      ).toBeInTheDocument();

      expect(
        screen.getAllByText('ananya.updated@example.com').length,
      ).toBeGreaterThan(0);

     expect(fetchCustomers).toHaveBeenCalledWith('', 0);
    expect(
      vi.mocked(fetchCustomers).mock.calls.length,
    ).toBeGreaterThanOrEqual(2);
    });

    it('displays an error and preserves changes when updating fails', async () => {
      const user = userEvent.setup();

      vi.mocked(updateCustomer).mockRejectedValue(
        new Error('Unable to update customer.'),
      );

      await openCustomerForEditing(user);

      await user.clear(
        screen.getByLabelText(/display name/i),
      );

      await user.type(
        screen.getByLabelText(/display name/i),
        'Ananya Rao Updated',
      );

      await user.click(
        screen.getByRole('button', { name: /save changes/i }),
      );

      expect(
        await screen.findByRole('alert'),
      ).toHaveTextContent('Unable to update customer.');

      expect(
        screen.getByRole('dialog'),
      ).toBeInTheDocument();

      expect(
        screen.getByLabelText(/display name/i),
      ).toHaveValue('Ananya Rao Updated');
    });
  });

  describe('customer archiving', () => {
    async function openCustomerForArchiving(
      user: ReturnType<typeof userEvent.setup>,
    ) {
      vi.mocked(fetchCustomer).mockResolvedValue(customer);

      render(<CustomersWorkspace />);

      await screen.findByText('Ananya Rao');

      await user.click(
        screen.getByRole('button', { name: /ananya rao/i }),
      );

      await waitFor(() => {
        expect(fetchCustomer).toHaveBeenCalledWith(customer.id);
      });

      await user.click(
        await screen.findByRole('button', {
          name: /^Archive$/,
        }),
      );

      expect(
        screen.getByRole('alertdialog'),
      ).toBeInTheDocument();
    }

    it('archives a customer successfully', async () => {
      const user = userEvent.setup();

      vi.mocked(archiveCustomer).mockResolvedValue(undefined);

      vi.mocked(fetchCustomers)
        .mockResolvedValueOnce(populatedPage)
        .mockResolvedValue(emptyPage);

      await openCustomerForArchiving(user);

      const confirmation = screen.getByRole('alertdialog');

      expect(
        screen.getByRole('heading', { name: 'Archive customer?' }),
      ).toBeInTheDocument();

      expect(confirmation).toHaveTextContent('Ananya Rao');
      expect(confirmation).toHaveTextContent('CUS-000001');

      await user.click(
        screen.getByRole('button', {
         name: /^Archive customer$/,
        }),
      );

      await waitFor(() => {
        expect(archiveCustomer).toHaveBeenCalledWith(customer.id);
      });

      await waitFor(() => {
        expect(
          screen.queryByRole('alertdialog'),
        ).not.toBeInTheDocument();
      });

      expect(
        await screen.findByText('No customers yet'),
      ).toBeInTheDocument();

      expect(
        screen.getByText('Customer details'),
      ).toBeInTheDocument();

      expect(fetchCustomers).toHaveBeenCalledWith('', 0);
     expect(
       vi.mocked(fetchCustomers).mock.calls.length,
     ).toBeGreaterThanOrEqual(2);
    });

    it('cancels archiving without making an API request', async () => {
      const user = userEvent.setup();

      await openCustomerForArchiving(user);

      await user.click(
        screen.getByRole('button', { name: /cancel/i }),
      );

      expect(
        screen.queryByRole('alertdialog'),
      ).not.toBeInTheDocument();

      expect(archiveCustomer).not.toHaveBeenCalled();

      expect(
        screen.getByRole('heading', { name: 'Ananya Rao' }),
      ).toBeInTheDocument();

      expect(
        screen.getByRole('button', {
         name: /^Archive$/,
        }),
      ).toBeInTheDocument();
    });

    it('displays an error and keeps the confirmation open when archiving fails', async () => {
      const user = userEvent.setup();

      vi.mocked(archiveCustomer).mockRejectedValue(
        new Error('Unable to archive customer.'),
      );

      await openCustomerForArchiving(user);

     await user.click(
       screen.getByRole('button', {
         name: /^Archive customer$/,
       }),
     );

      expect(
        await screen.findByRole('alert'),
      ).toHaveTextContent('Unable to archive customer.');

      const confirmation = screen.getByRole('alertdialog');

      expect(confirmation).toBeInTheDocument();

      expect(
        screen.getByRole('heading', { name: 'Archive customer?' }),
      ).toBeInTheDocument();

      expect(confirmation).toHaveTextContent('Ananya Rao');
      expect(confirmation).toHaveTextContent('CUS-000001');

      expect(
        screen.getByRole('button', {
         name: /^Archive$/,
        }),
      ).toBeInTheDocument();

      expect(archiveCustomer).toHaveBeenCalledWith(customer.id);
    });
  });
});
