
'use client';

import { useEffect, useState } from 'react';

import {
  fetchActiveOrganization,
  fetchOrganizations,
  selectActiveOrganization,
  OrganizationApiError,
  type OrganizationMembership,
} from '@/features/organization/api/organization-api';

import Link from 'next/link';

import styles from './organization-switcher.module.css';

function getOrganizationLabel(
  organization: OrganizationMembership,
): string {
  return (
    organization.displayName?.trim() ||
    organization.organizationName
  );
}

function getRoleLabel(roles: string[]): string {
  if (roles.length === 0) {
    return 'Member';
  }

  return roles
    .map((role) =>
      role
        .toLowerCase()
        .replace(/_/g, ' ')
        .replace(/\b\w/g, (letter) => letter.toUpperCase()),
    )
    .join(', ');
}

function getErrorMessage(exception: unknown): string {
  if (
    exception instanceof OrganizationApiError &&
    exception.status === 401
  ) {
    return 'Your session has expired. Please sign in again.';
  }

  return exception instanceof Error
    ? exception.message
    : 'Unable to load your organizations.';
}

export default function OrganizationSwitcher() {
  const [organizations, setOrganizations] = useState<
    OrganizationMembership[]
  >([]);

  const [activeOrganization, setActiveOrganization] =
    useState<OrganizationMembership | null>(null);

  const [loading, setLoading] = useState(true);
  const [switching, setSwitching] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    let cancelled = false;

    Promise.all([
      fetchOrganizations(),
      fetchActiveOrganization(),
    ])
      .then(([availableOrganizations, selectedOrganization]) => {
        if (cancelled) {
          return;
        }

        setOrganizations(availableOrganizations);

        const validSelection =
          selectedOrganization &&
          availableOrganizations.some(
            (organization) =>
              organization.organizationId ===
              selectedOrganization.organizationId,
          )
            ? selectedOrganization
            : null;

        setActiveOrganization(validSelection);
        setError('');
      })
      .catch((exception: unknown) => {
        if (cancelled) {
          return;
        }

        setError(getErrorMessage(exception));
      })
      .finally(() => {
        if (!cancelled) {
          setLoading(false);
        }
      });

    return () => {
      cancelled = true;
    };
  }, []);

  async function handleOrganizationChange(
    organizationId: string,
  ) {
    if (!organizationId || switching) {
      return;
    }

    setSwitching(true);
    setError('');

    try {
      const selected = await selectActiveOrganization(
        organizationId,
      );

      setActiveOrganization(selected);
    } catch (exception) {
      setError(getErrorMessage(exception));
    } finally {
      setSwitching(false);
    }
  }

  async function handleRetry() {
    setLoading(true);
    setError('');

    try {
      const [availableOrganizations, selectedOrganization] =
        await Promise.all([
          fetchOrganizations(),
          fetchActiveOrganization(),
        ]);

      setOrganizations(availableOrganizations);

      const validSelection =
        selectedOrganization &&
        availableOrganizations.some(
          (organization) =>
            organization.organizationId ===
            selectedOrganization.organizationId,
        )
          ? selectedOrganization
          : null;

      setActiveOrganization(validSelection);
    } catch (exception) {
      setError(getErrorMessage(exception));
    } finally {
      setLoading(false);
    }
  }

  if (loading) {
    return (
      <div className={styles.container}>
        <div className={styles.avatar}>A</div>
        <div className={styles.copy}>
          <strong>Loading workspace…</strong>
          <span>Please wait</span>
        </div>
      </div>
    );
  }

  if (error && organizations.length === 0) {
    return (
      <div className={styles.container}>
        <div className={styles.avatar}>!</div>
        <div className={styles.copy}>
          <strong>Workspace unavailable</strong>
          <span>{error}</span>
          <button
            type="button"
            className={styles.retryButton}
            onClick={() => void handleRetry()}
          >
            Retry
          </button>
        </div>
      </div>
    );
  }

  if (organizations.length === 0) {
    return (
      <div className={styles.container}>
        <div className={styles.avatar}>A</div>

        <div className={styles.copy}>
          <strong>No organization yet</strong>
          <span>Create an organization to get started.</span>

          <Link
            href="/app/organizations/new"
            className={styles.createButton}
          >
            Create organization
          </Link>
        </div>
      </div>
    );
  }

  const selectedId = activeOrganization?.organizationId ?? '';

  return (
    <div className={styles.wrapper}>
      <div className={styles.container}>
        <div className={styles.avatar}>
          {(
            activeOrganization
              ? getOrganizationLabel(activeOrganization)
              : 'A'
          )
            .charAt(0)
            .toUpperCase()}
        </div>

        <div className={styles.copy}>
          <label
            className={styles.label}
            htmlFor="organization-selector"
          >
            ORGANIZATION
          </label>

          <select
            id="organization-selector"
            className={styles.selector}
            value={selectedId}
            disabled={switching}
            onChange={(event) =>
              void handleOrganizationChange(event.target.value)
            }
          >
            <option value="" disabled>
              Select organization
            </option>

            {organizations.map((organization) => (
              <option
                key={organization.organizationId}
                value={organization.organizationId}
              >
                {getOrganizationLabel(organization)}
              </option>
            ))}
          </select>

          <span className={styles.role}>
            {switching
              ? 'Switching workspace…'
              : activeOrganization
                ? getRoleLabel(activeOrganization.roles)
                : 'No active workspace selected'}
          </span>
        </div>
      </div>

      {error && (
        <p className={styles.error} role="alert">
          {error}
        </p>
      )}
    </div>
  );
}
