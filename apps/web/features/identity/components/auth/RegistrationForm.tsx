
'use client';

import { useState, type FormEvent } from 'react';
import Link from 'next/link';

import AuthPageShell from './AuthPageShell';
import styles from './auth.module.css';

import {
  IdentityApiError,
  registerIdentity,
} from '@/features/identity/api/identity-api';

export default function RegistrationForm() {
  const [displayName, setDisplayName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');

  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');
  const [successMessage, setSuccessMessage] = useState('');

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    setError('');
    setSuccessMessage('');

    const normalizedName = displayName.trim();
    const normalizedEmail = email.trim();
    const passwordLength = Array.from(
      password.normalize('NFC'),
    ).length;

    if (!normalizedName) {
      setError('Please enter your name.');
      return;
    }

    if (passwordLength < 15 || passwordLength > 128) {
      setError(
        'Your password must contain between 15 and 128 characters.',
      );
      return;
    }

    if (password !== confirmPassword) {
      setError('Your passwords do not match.');
      return;
    }

    setSubmitting(true);

    try {
      const message = await registerIdentity(
        normalizedName,
        normalizedEmail,
        password,
      );

      setSuccessMessage(message);
      setPassword('');
      setConfirmPassword('');
    } catch (exception) {
      if (
        exception instanceof IdentityApiError &&
        exception.status === 429
      ) {
        setError(
          'Too many registration attempts. Please wait before trying again.',
        );
      } else if (exception instanceof Error) {
        setError(exception.message);
      } else {
        setError(
          'Unable to complete registration. Please try again.',
        );
      }
    } finally {
      setSubmitting(false);
    }
  }

  if (successMessage) {
    return (
      <AuthPageShell
        eyebrow="REGISTRATION RECEIVED"
        title="Check your inbox."
        description="We've received your registration request."
        footer={
          <Link href="/login" className={styles.textLink}>
            Return to sign in
          </Link>
        }
      >
        <div className={styles.successMessage} role="status">
          {successMessage}
        </div>

        <p className={styles.helperText}>
          Follow the instructions in the email to verify your
          address and activate your Arovia account. If the email
          does not arrive, check your spam folder.
        </p>
      </AuthPageShell>
    );
  }

  return (
    <AuthPageShell
      eyebrow="GET STARTED"
      title="Create your Arovia account."
      description="Start bringing your business operations together."
      footer={
        <>
          <span className={styles.formFooterText}>
            Already have an account?
          </span>
          <Link href="/login" className={styles.textLink}>
            Sign in
          </Link>
        </>
      }
    >
      <form className={styles.form} onSubmit={handleSubmit}>
        <div className={styles.field}>
          <label className={styles.label} htmlFor="signup-name">
            Your name
          </label>

          <input
            id="signup-name"
            className={styles.input}
            type="text"
            autoComplete="name"
            placeholder="Your full name"
            value={displayName}
            onChange={(event) => setDisplayName(event.target.value)}
            required
            maxLength={120}
            disabled={submitting}
          />
        </div>

        <div className={styles.field}>
          <label className={styles.label} htmlFor="signup-email">
            Email address
          </label>

          <input
            id="signup-email"
            className={styles.input}
            type="email"
            autoComplete="email"
            placeholder="you@company.com"
            value={email}
            onChange={(event) => setEmail(event.target.value)}
            required
            maxLength={254}
            disabled={submitting}
          />
        </div>

        <div className={styles.field}>
          <label className={styles.label} htmlFor="signup-password">
            Password
          </label>

          <input
            id="signup-password"
            className={styles.input}
            type="password"
            autoComplete="new-password"
            placeholder="Create a strong password"
            value={password}
            onChange={(event) => setPassword(event.target.value)}
            required
            maxLength={256}
            disabled={submitting}
          />

          <p className={styles.passwordRequirements}>
            Use 15–128 characters. Avoid passwords containing
            your email address or commonly used phrases.
          </p>
        </div>

        <div className={styles.field}>
          <label
            className={styles.label}
            htmlFor="signup-confirm-password"
          >
            Confirm password
          </label>

          <input
            id="signup-confirm-password"
            className={styles.input}
            type="password"
            autoComplete="new-password"
            placeholder="Enter your password again"
            value={confirmPassword}
            onChange={(event) =>
              setConfirmPassword(event.target.value)
            }
            required
            maxLength={256}
            disabled={submitting}
          />
        </div>

        {error && (
          <div className={styles.errorMessage} role="alert">
            {error}
          </div>
        )}

        <button
          className={styles.primaryButton}
          type="submit"
          disabled={
            submitting ||
            !displayName.trim() ||
            !email.trim() ||
            !password ||
            !confirmPassword
          }
        >
          {submitting ? 'Creating account…' : 'Create account'}
        </button>
      </form>
    </AuthPageShell>
  );
}
