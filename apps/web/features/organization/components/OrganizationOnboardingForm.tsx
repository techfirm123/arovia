
'use client';

import { useState, type FormEvent } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';

import {
  createOrganization,
  selectActiveOrganization,
  OrganizationApiError,
} from '@/features/organization/api/organization-api';

import styles from './organization-onboarding.module.css';

export default function OrganizationOnboardingForm() {
  const router = useRouter();

  const [businessName, setBusinessName] = useState('');
  const [displayName, setDisplayName] = useState('');
  const [legalName, setLegalName] = useState('');

  const [countryCode, setCountryCode] = useState('IN');
  const [currency, setCurrency] = useState('INR');
  const [timezone, setTimezone] = useState('Asia/Kolkata');

  const [locationName, setLocationName] = useState('Main Branch');
  const [addressLine1, setAddressLine1] = useState('');
  const [addressLine2, setAddressLine2] = useState('');
  const [city, setCity] = useState('');
  const [stateOrRegion, setStateOrRegion] = useState('');
  const [postalCode, setPostalCode] = useState('');

  const [contactEmail, setContactEmail] = useState('');
  const [contactPhone, setContactPhone] = useState('');
  const [websiteUrl, setWebsiteUrl] = useState('');

  const [includeLocation, setIncludeLocation] = useState(true);

  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');
  const [createdButNotSelected, setCreatedButNotSelected] =
    useState(false);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    setError('');
    setCreatedButNotSelected(false);

    const normalizedName = businessName.trim();

    if (!normalizedName) {
      setError('Please enter your business name.');
      return;
    }

    if (includeLocation && !locationName.trim()) {
      setError('Please enter a name for your primary location.');
      return;
    }

    setSubmitting(true);

    try {
      const organization = await createOrganization({
        name: normalizedName,
        profile: {
          displayName: displayName.trim() || normalizedName,
          legalName: legalName.trim(),
          countryCode,
          defaultCurrency: currency,
          timezone,
          websiteUrl: websiteUrl.trim() || undefined,
          contactEmail: contactEmail.trim() || undefined,
          contactPhone: contactPhone.trim() || undefined,
        },
        initialLocation: includeLocation
          ? {
              name: locationName.trim(),
              locationType: 'BUSINESS',
              addressLine1: addressLine1.trim() || undefined,
              addressLine2: addressLine2.trim() || undefined,
              city: city.trim() || undefined,
              stateOrRegion: stateOrRegion.trim() || undefined,
              postalCode: postalCode.trim() || undefined,
              countryCode,
              timezone,
            }
          : null,
      });

      try {
        await selectActiveOrganization(organization.id);
      } catch {
        setCreatedButNotSelected(true);
        setError(
          'Your organization was created, but we could not select it automatically. You can select it from the workspace switcher.',
        );
        return;
      }

      router.replace('/app');
      router.refresh();
    } catch (exception) {
      if (
        exception instanceof OrganizationApiError &&
        exception.status === 401
      ) {
        setError('Your session has expired. Please sign in again.');
      } else if (
        exception instanceof OrganizationApiError &&
        exception.status === 403
      ) {
        setError(
          'Please verify your email address before creating an organization.',
        );
      } else if (exception instanceof Error) {
        setError(exception.message);
      } else {
        setError('Unable to create your organization. Please try again.');
      }
    } finally {
      setSubmitting(false);
    }
  }

  if (createdButNotSelected) {
    return (
      <div className={styles.successPanel} role="status">
        <div className={styles.successIcon}>✓</div>
        <h2>Your business has been created.</h2>
        <p>{error}</p>
        <Link href="/app" className={styles.primaryButton}>
          Return to workspace
        </Link>
      </div>
    );
  }

  return (
    <form className={styles.form} onSubmit={handleSubmit}>
      <section className={styles.section}>
        <div className={styles.sectionHeading}>
          <span className={styles.step}>01</span>
          <div>
            <h2>Tell us about your business</h2>
            <p>Start with the name people know your business by.</p>
          </div>
        </div>

        <div className={styles.fieldGrid}>
          <div className={styles.field}>
            <label htmlFor="business-name">Business name *</label>
            <input
              id="business-name"
              value={businessName}
              onChange={(event) => setBusinessName(event.target.value)}
              placeholder="e.g. Acme Computers"
              maxLength={160}
              required
              disabled={submitting}
            />
          </div>

          <div className={styles.field}>
            <label htmlFor="display-name">Display name</label>
            <input
              id="display-name"
              value={displayName}
              onChange={(event) => setDisplayName(event.target.value)}
              placeholder="Name shown across Arovia"
              maxLength={160}
              disabled={submitting}
            />
          </div>

          <div className={styles.field}>
            <label htmlFor="legal-name">Legal business name</label>
            <input
              id="legal-name"
              value={legalName}
              onChange={(event) => setLegalName(event.target.value)}
              placeholder="Registered legal name"
              maxLength={200}
              disabled={submitting}
            />
          </div>

          <div className={styles.field}>
            <label htmlFor="business-email">Business email</label>
            <input
              id="business-email"
              type="email"
              value={contactEmail}
              onChange={(event) => setContactEmail(event.target.value)}
              placeholder="contact@business.com"
              maxLength={320}
              disabled={submitting}
            />
          </div>

          <div className={styles.field}>
            <label htmlFor="business-phone">Business phone</label>
            <input
              id="business-phone"
              type="tel"
              value={contactPhone}
              onChange={(event) => setContactPhone(event.target.value)}
              placeholder="Contact number"
              maxLength={40}
              disabled={submitting}
            />
          </div>

          <div className={styles.field}>
            <label htmlFor="business-website">Website</label>
            <input
              id="business-website"
              type="url"
              value={websiteUrl}
              onChange={(event) => setWebsiteUrl(event.target.value)}
              placeholder="https://example.com"
              maxLength={500}
              disabled={submitting}
            />
          </div>
        </div>
      </section>

      <section className={styles.section}>
        <div className={styles.sectionHeading}>
          <span className={styles.step}>02</span>
          <div>
            <h2>Regional preferences</h2>
            <p>Set the defaults used for your business operations.</p>
          </div>
        </div>

        <div className={styles.fieldGrid}>
          <div className={styles.field}>
            <label htmlFor="country-code">Country code *</label>
            <select
              id="country-code"
              value={countryCode}
              onChange={(event) => setCountryCode(event.target.value)}
              disabled={submitting}
              required
            >
              <option value="IN">India (IN)</option>
              <option value="US">United States (US)</option>
              <option value="GB">United Kingdom (GB)</option>
              <option value="AE">United Arab Emirates (AE)</option>
              <option value="SG">Singapore (SG)</option>
              <option value="AU">Australia (AU)</option>
              <option value="CA">Canada (CA)</option>
            </select>
          </div>

          <div className={styles.field}>
            <label htmlFor="currency">Default currency *</label>
            <select
              id="currency"
              value={currency}
              onChange={(event) => setCurrency(event.target.value)}
              disabled={submitting}
              required
            >
              <option value="INR">INR — Indian Rupee</option>
              <option value="USD">USD — US Dollar</option>
              <option value="GBP">GBP — British Pound</option>
              <option value="AED">AED — UAE Dirham</option>
              <option value="SGD">SGD — Singapore Dollar</option>
              <option value="AUD">AUD — Australian Dollar</option>
              <option value="CAD">CAD — Canadian Dollar</option>
            </select>
          </div>

          <div className={styles.field}>
            <label htmlFor="timezone">Timezone *</label>
            <select
              id="timezone"
              value={timezone}
              onChange={(event) => setTimezone(event.target.value)}
              disabled={submitting}
              required
            >
              <option value="Asia/Kolkata">Asia/Kolkata</option>
              <option value="Asia/Dubai">Asia/Dubai</option>
              <option value="Asia/Singapore">Asia/Singapore</option>
              <option value="Europe/London">Europe/London</option>
              <option value="America/New_York">America/New_York</option>
              <option value="America/Los_Angeles">America/Los_Angeles</option>
              <option value="Australia/Sydney">Australia/Sydney</option>
            </select>
          </div>
        </div>
      </section>

      <section className={styles.section}>
        <div className={styles.sectionHeading}>
          <span className={styles.step}>03</span>
          <div>
            <h2>Primary business location</h2>
            <p>
              Add your main branch or skip this for now and add locations
              later.
            </p>
          </div>
        </div>

        <label className={styles.checkboxRow}>
          <input
            type="checkbox"
            checked={includeLocation}
            onChange={(event) => setIncludeLocation(event.target.checked)}
            disabled={submitting}
          />
          <span>Create a primary business location now</span>
        </label>

        {includeLocation && (
          <div className={styles.fieldGrid}>
            <div className={styles.field}>
              <label htmlFor="location-name">Location name *</label>
              <input
                id="location-name"
                value={locationName}
                onChange={(event) => setLocationName(event.target.value)}
                placeholder="Main Branch"
                maxLength={120}
                required
                disabled={submitting}
              />
            </div>

            <div className={styles.field}>
              <label htmlFor="location-city">City</label>
              <input
                id="location-city"
                value={city}
                onChange={(event) => setCity(event.target.value)}
                placeholder="City"
                maxLength={120}
                disabled={submitting}
              />
            </div>

            <div className={styles.field}>
              <label htmlFor="location-address-1">Address line 1</label>
              <input
                id="location-address-1"
                value={addressLine1}
                onChange={(event) => setAddressLine1(event.target.value)}
                placeholder="Street address"
                maxLength={200}
                disabled={submitting}
              />
            </div>

            <div className={styles.field}>
              <label htmlFor="location-address-2">Address line 2</label>
              <input
                id="location-address-2"
                value={addressLine2}
                onChange={(event) => setAddressLine2(event.target.value)}
                placeholder="Building, area, landmark"
                maxLength={200}
                disabled={submitting}
              />
            </div>

            <div className={styles.field}>
              <label htmlFor="location-state">State or region</label>
              <input
                id="location-state"
                value={stateOrRegion}
                onChange={(event) => setStateOrRegion(event.target.value)}
                placeholder="State or region"
                maxLength={120}
                disabled={submitting}
              />
            </div>

            <div className={styles.field}>
              <label htmlFor="location-postal">Postal code</label>
              <input
                id="location-postal"
                value={postalCode}
                onChange={(event) => setPostalCode(event.target.value)}
                placeholder="Postal code"
                maxLength={24}
                disabled={submitting}
              />
            </div>
          </div>
        )}
      </section>

      {error && (
        <div className={styles.errorMessage} role="alert">
          {error}
        </div>
      )}

      <div className={styles.actions}>
        <Link href="/app" className={styles.cancelButton}>
          Cancel
        </Link>

        <button
          type="submit"
          className={styles.primaryButton}
          disabled={submitting || !businessName.trim()}
        >
          {submitting ? 'Creating workspace…' : 'Create business workspace'}
        </button>
      </div>

      <p className={styles.footnote}>
        You will become the OWNER of this organization. You can invite
        teammates and configure additional locations later.
      </p>
    </form>
  );
}
