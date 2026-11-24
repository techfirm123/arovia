
'use client';

import { useCallback, useEffect, useState } from 'react';
import type { FormEvent } from 'react';
import {
  Archive,
  Building2,
  ChevronLeft,
  ChevronRight,
  CircleUserRound,
  LoaderCircle,
  Pencil,
  Plus,
  Search,
  Users,
  X,
} from 'lucide-react';

import {
  archiveCustomer,
  createCustomer,
  fetchCustomer,
  fetchCustomers,
  updateCustomer,
  CustomerApiError,
  type Customer,
  type CustomerPage,
  type CustomerPayload,
  type CustomerType,
} from '@/features/customer/api/customer-api';

import styles from './customers-workspace.module.css';

type FormState = {
  customerType: CustomerType;
  displayName: string;
  legalName: string;
  email: string;
  phone: string;
  notes: string;
};

const EMPTY_FORM: FormState = {
  customerType: 'INDIVIDUAL',
  displayName: '',
  legalName: '',
  email: '',
  phone: '',
  notes: '',
};

function toPayload(form: FormState): CustomerPayload {
  return {
    customerType: form.customerType,
    displayName: form.displayName.trim(),
    legalName: form.legalName.trim() || null,
    email: form.email.trim() || null,
    phone: form.phone.trim() || null,
    notes: form.notes.trim() || null,
  };
}

function toForm(customer: Customer): FormState {
  return {
    customerType: customer.customerType,
    displayName: customer.displayName,
    legalName: customer.legalName ?? '',
    email: customer.email ?? '',
    phone: customer.phone ?? '',
    notes: customer.notes ?? '',
  };
}

function getErrorMessage(exception: unknown): string {
  if (
    exception instanceof CustomerApiError &&
    exception.status === 401
  ) {
    return 'Your session has expired. Please sign in again.';
  }

  if (
    exception instanceof CustomerApiError &&
    exception.status === 409
  ) {
    return 'Select an organization before managing customers.';
  }

  return exception instanceof Error
    ? exception.message
    : 'Something went wrong. Please try again.';
}

function formatDate(value: string): string {
  return new Intl.DateTimeFormat('en-IN', {
    day: 'numeric',
    month: 'short',
    year: 'numeric',
  }).format(new Date(value));
}

export default function CustomersWorkspace() {
  const [data, setData] = useState<CustomerPage | null>(null);
  const [search, setSearch] = useState('');
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [archiving, setArchiving] = useState(false);
  const [error, setError] = useState('');

  const [selected, setSelected] = useState<Customer | null>(null);
  const [formOpen, setFormOpen] = useState(false);
  const [editing, setEditing] = useState<Customer | null>(null);
  const [form, setForm] = useState<FormState>(EMPTY_FORM);
  const [archiveTarget, setArchiveTarget] = useState<Customer | null>(null);

  const loadCustomers = useCallback(async () => {
    setLoading(true);
    setError('');

    try {
      const result = await fetchCustomers(search, page);
      setData(result);

      setSelected((current) =>
        current &&
        !result.items.some((item) => item.id === current.id)
          ? null
          : current,
      );
    } catch (exception) {
      setError(getErrorMessage(exception));
    } finally {
      setLoading(false);
    }
  }, [page, search]);

  useEffect(() => {
    const timeout = window.setTimeout(() => {
      void loadCustomers();
    }, 250);

    return () => window.clearTimeout(timeout);
  }, [loadCustomers]);

  async function openCustomer(customerId: string) {
    setError('');

    try {
      const customer = await fetchCustomer(customerId);
      setSelected(customer);
    } catch (exception) {
      setError(getErrorMessage(exception));
    }
  }

  function openCreateForm() {
    setEditing(null);
    setForm(EMPTY_FORM);
    setFormOpen(true);
  }

  function openEditForm(customer: Customer) {
    setEditing(customer);
    setForm(toForm(customer));
    setFormOpen(true);
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    if (saving) {
      return;
    }

    setSaving(true);
    setError('');

    try {
      const payload = toPayload(form);

      const saved = editing
        ? await updateCustomer(editing.id, payload)
        : await createCustomer(payload);

      setSelected(saved);
      setFormOpen(false);
      setEditing(null);
      setForm(EMPTY_FORM);
      await loadCustomers();
    } catch (exception) {
      setError(getErrorMessage(exception));
    } finally {
      setSaving(false);
    }
  }

  async function confirmArchive() {
    if (!archiveTarget || archiving) {
      return;
    }

    setArchiving(true);
    setError('');

    try {
      await archiveCustomer(archiveTarget.id);

      setSelected((current) =>
        current?.id === archiveTarget.id ? null : current
      );

      setArchiveTarget(null);
      await loadCustomers();
    } catch (exception) {
      setError(getErrorMessage(exception));
    } finally {
      setArchiving(false);
    }
  }

  function updateForm<K extends keyof FormState>(
    key: K,
    value: FormState[K],
  ) {
    setForm((current) => ({
      ...current,
      [key]: value,
    }));
  }

  const customers = data?.items ?? [];

  return (
    <div className={styles.workspace}>
      <div className={styles.heading}>
        <div>
          <div className={styles.eyebrow}>CUSTOMER MANAGEMENT</div>
          <h1>Customers<span>.</span></h1>
          <p>Keep customer information organized across your business.</p>
        </div>

        <button
          type="button"
          className={styles.primaryButton}
          onClick={openCreateForm}
        >
          <Plus size={16} />
          New customer
        </button>
      </div>

      <div className={styles.toolbar}>
        <div className={styles.searchBox}>
          <Search size={17} />
          <input
            type="search"
            value={search}
            placeholder="Search name, customer number, email or phone"
            onChange={(event) => {
              setPage(0);
              setSearch(event.target.value);
            }}
            aria-label="Search customers"
          />
          {search && (
            <button
              type="button"
              aria-label="Clear search"
              onClick={() => {
                setPage(0);
                setSearch('');
              }}
            >
              <X size={15} />
            </button>
          )}
        </div>

        <span className={styles.resultCount}>
          {data?.totalElements ?? 0} customers
        </span>
      </div>

      {error && (
        <div className={styles.error} role="alert">
          {error}
        </div>
      )}

      <div className={styles.contentGrid}>
        <section className={styles.directory}>
          <div className={styles.sectionHeader}>
            <div>
              <h2>Customer directory</h2>
              <p>Active customers in this organization</p>
            </div>
            {loading && <LoaderCircle className={styles.spinner} size={18} />}
          </div>

          {loading && customers.length === 0 ? (
            <div className={styles.emptyState}>
              <LoaderCircle className={styles.spinner} size={24} />
              <span>Loading customers…</span>
            </div>
          ) : customers.length === 0 ? (
            <div className={styles.emptyState}>
              <div className={styles.emptyIcon}><Users size={23} /></div>
              <strong>{search ? 'No matching customers' : 'No customers yet'}</strong>
              <span>
                {search
                  ? 'Try another name, number, email or phone.'
                  : 'Create your first customer to start building your directory.'}
              </span>
              {!search && (
                <button
                  type="button"
                  className={styles.secondaryButton}
                  onClick={openCreateForm}
                >
                  <Plus size={15} /> Add customer
                </button>
              )}
            </div>
          ) : (
            <div className={styles.customerList}>
              {customers.map((customer) => (
                <button
                  type="button"
                  key={customer.id}
                  className={`${styles.customerRow} ${selected?.id === customer.id ? styles.customerRowSelected : ''}`}
                  onClick={() => void openCustomer(customer.id)}
                >
                  <div className={styles.customerAvatar}>
                    {customer.customerType === 'BUSINESS'
                      ? <Building2 size={18} />
                      : <CircleUserRound size={18} />}
                  </div>

                  <div className={styles.customerMain}>
                    <strong>{customer.displayName}</strong>
                    <span>{customer.customerNumber}</span>
                  </div>

                  <div className={styles.customerContact}>
                    <span>{customer.email || 'No email'}</span>
                    <span>{customer.phone || 'No phone'}</span>
                  </div>

                  <span className={styles.typePill}>
                    {customer.customerType === 'BUSINESS'
                      ? 'Business'
                      : 'Individual'}
                  </span>
                </button>
              ))}
            </div>
          )}

          <div className={styles.pagination}>
            <span>
              {data && data.totalElements > 0
                ? `Page ${data.page + 1} of ${Math.max(data.totalPages, 1)}`
                : 'No results'}
            </span>
            <div>
              <button
                type="button"
                aria-label="Previous page"
                disabled={!data || data.page <= 0 || loading}
                onClick={() => setPage((current) => Math.max(0, current - 1))}
              >
                <ChevronLeft size={17} />
              </button>
              <button
                type="button"
                aria-label="Next page"
                disabled={!data || data.page + 1 >= data.totalPages || loading}
                onClick={() => setPage((current) => current + 1)}
              >
                <ChevronRight size={17} />
              </button>
            </div>
          </div>
        </section>

        <aside className={styles.details}>
          {selected ? (
            <>
              <div className={styles.detailHeader}>
                <div className={styles.detailAvatar}>
                  {selected.customerType === 'BUSINESS'
                    ? <Building2 size={22} />
                    : <CircleUserRound size={22} />}
                </div>
                <div>
                  <span className={styles.eyebrow}>CUSTOMER PROFILE</span>
                  <h2>{selected.displayName}</h2>
                  <span className={styles.customerNumber}>
                    {selected.customerNumber}
                  </span>
                </div>
              </div>

              <div className={styles.detailActions}>
                <button
                  type="button"
                  className={styles.secondaryButton}
                  onClick={() => openEditForm(selected)}
                >
                  <Pencil size={14} /> Edit
                </button>
                {selected.status === 'ACTIVE' && (
                  <button
                    type="button"
                    className={styles.archiveButton}
                    onClick={() => setArchiveTarget(selected)}
                  >
                    <Archive size={14} /> Archive
                  </button>
                )}
              </div>

              <div className={styles.detailSection}>
                <h3>Contact information</h3>
                <dl>
                  <div><dt>Customer type</dt><dd>{selected.customerType === 'BUSINESS' ? 'Business' : 'Individual'}</dd></div>
                  <div><dt>Legal name</dt><dd>{selected.legalName || '—'}</dd></div>
                  <div><dt>Email</dt><dd>{selected.email || '—'}</dd></div>
                  <div><dt>Phone</dt><dd>{selected.phone || '—'}</dd></div>
                </dl>
              </div>

              <div className={styles.detailSection}>
                <h3>Notes</h3>
                <p className={styles.notes}>
                  {selected.notes || 'No notes have been added.'}
                </p>
              </div>

              <div className={styles.detailFooter}>
                <span>Created {formatDate(selected.createdAt)}</span>
                <span className={selected.status === 'ACTIVE' ? styles.activeStatus : styles.archivedStatus}>
                  {selected.status}
                </span>
              </div>
            </>
          ) : (
            <div className={styles.detailEmpty}>
              <div className={styles.emptyIcon}><CircleUserRound size={24} /></div>
              <strong>Customer details</strong>
              <span>Select a customer from the directory to view their profile.</span>
            </div>
          )}
        </aside>
      </div>

      {formOpen && (
        <div className={styles.modalBackdrop}>
          <section
            className={styles.modal}
            role="dialog"
            aria-modal="true"
            aria-labelledby="customer-form-title"
          >
            <div className={styles.modalHeader}>
              <div>
                <span className={styles.eyebrow}>
                  CUSTOMER MANAGEMENT
                </span>
                <h2 id="customer-form-title">
                  {editing ? 'Edit customer' : 'New customer'}
                </h2>
              </div>
              <button
                type="button"
                className={styles.closeButton}
                aria-label="Close"
                onClick={() => setFormOpen(false)}
              >
                <X size={19} />
              </button>
            </div>

            <form onSubmit={(event) => void handleSubmit(event)}>
              <div className={styles.formGrid}>
                <label className={styles.fullWidth}>
                  Customer type
                  <select
                    value={form.customerType}
                    onChange={(event) =>
                      updateForm('customerType', event.target.value as CustomerType)
                    }
                  >
                    <option value="INDIVIDUAL">Individual</option>
                    <option value="BUSINESS">Business</option>
                  </select>
                </label>

                <label className={styles.fullWidth}>
                  Display name *
                  <input
                    required
                    maxLength={160}
                    value={form.displayName}
                    onChange={(event) => updateForm('displayName', event.target.value)}
                    placeholder="Customer name"
                  />
                </label>

                <label className={styles.fullWidth}>
                  Legal name
                  <input
                    maxLength={200}
                    value={form.legalName}
                    onChange={(event) => updateForm('legalName', event.target.value)}
                    placeholder="Registered legal name (optional)"
                  />
                </label>

                <label>
                  Email
                  <input
                    type="email"
                    maxLength={320}
                    value={form.email}
                    onChange={(event) => updateForm('email', event.target.value)}
                    placeholder="name@example.com"
                  />
                </label>

                <label>
                  Phone
                  <input
                    type="tel"
                    maxLength={40}
                    value={form.phone}
                    onChange={(event) => updateForm('phone', event.target.value)}
                    placeholder="+91"
                  />
                </label>

                <label className={styles.fullWidth}>
                  Notes
                  <textarea
                    rows={3}
                    maxLength={2000}
                    value={form.notes}
                    onChange={(event) => updateForm('notes', event.target.value)}
                    placeholder="Additional information about this customer"
                  />
                </label>
              </div>

              <div className={styles.modalFooter}>
                <button
                  type="button"
                  className={styles.cancelButton}
                  onClick={() => setFormOpen(false)}
                  disabled={saving}
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className={styles.primaryButton}
                  disabled={saving}
                >
                  {saving && <LoaderCircle className={styles.spinner} size={15} />}
                  {saving
                    ? 'Saving…'
                    : editing
                      ? 'Save changes'
                      : 'Create customer'}
                </button>
              </div>
            </form>
          </section>
        </div>
      )}

      {archiveTarget && (
        <div
          className={styles.modalBackdrop}
          onMouseDown={(event) => {
            if (event.target === event.currentTarget && !archiving) {
              setArchiveTarget(null);
            }
          }}
        >
          <section
            className={styles.confirmDialog}
            role="alertdialog"
            aria-modal="true"
            aria-labelledby="archive-customer-title"
            aria-describedby="archive-customer-description"
          >
            <div className={styles.confirmIcon}>
              <Archive size={21} />
            </div>

            <div className={styles.confirmContent}>
              <span className={styles.eyebrow}>CUSTOMER MANAGEMENT</span>

              <h2 id="archive-customer-title">
                Archive customer?
              </h2>

              <p id="archive-customer-description">
                <strong>{archiveTarget.displayName}</strong>
                {' '}({archiveTarget.customerNumber}) will no longer appear
                in the active customer directory. Existing customer
                information will be retained.
              </p>
            </div>

            <div className={styles.confirmFooter}>
              <button
                type="button"
                className={styles.cancelButton}
                disabled={archiving}
                onClick={() => setArchiveTarget(null)}
              >
                Cancel
              </button>

              <button
                type="button"
                className={styles.archiveButton}
                disabled={archiving}
                onClick={() => void confirmArchive()}
              >
                {archiving && (
                  <LoaderCircle
                    size={14}
                    className={styles.spinner}
                  />
                )}
                {archiving ? 'Archiving…' : 'Archive customer'}
              </button>
            </div>
          </section>
        </div>
      )}
    </div>
  );
}
