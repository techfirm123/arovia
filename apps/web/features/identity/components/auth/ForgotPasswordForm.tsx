'use client';

import { useState, type FormEvent } from 'react';
import Link from 'next/link';

import AuthPageShell from './AuthPageShell';
import styles from './auth.module.css';

import {
  IdentityApiError,
  requestPasswordReset,
} from '@/features/identity/api/identity-api';

export default function ForgotPasswordForm() {
  const [email, setEmail] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const [completed, setCompleted] = useState(false);
  const [error, setError] = useState('');

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    setError('');
    setSubmitting(true);

    try {
      await requestPasswordReset(email.trim());

      setCompleted(true);

    } catch (exception) {
      if (exception instanceof IdentityApiError) {
        setError(exception.message);
      } else {
        setError(
          'We could not submit your request. Please try again.',
        );
      }
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <AuthPageShell
      eyebrow="ACCOUNT RECOVERY"
      title="Forgot your password?"
      description={
        completed
          ? 'Check your inbox for the next steps.'
          : 'Enter the email address associated with your Arovia account.'
      }
    >
      {completed ? (
        <>
          <div className={styles.successMessage} role="status">
            If an account can be recovered for this email address,
            you will receive a password reset link.

            <br />
            <br />

            Check your inbox, including the spam or junk folder.
            The reset link expires after 30 minutes.
          </div>

          <Link
            href="/login"
            className={styles.centerLink}
          >
            Return to sign in
          </Link>
        </>
      ) : (
        <>
          <form
            className={styles.form}
            onSubmit={handleSubmit}
          >
            <div className={styles.field}>
              <label
                className={styles.label}
                htmlFor="reset-email"
              >
                Email address
              </label>

              <input
                id="reset-email"
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

            {error && (
              <div className={styles.errorMessage} role="alert">
                {error}
              </div>
            )}

            <button
              className={styles.primaryButton}
              type="submit"
              disabled={submitting || !email.trim()}
            >
              {submitting
                ? 'Submitting…'
                : 'Send password reset link'}
            </button>
          </form>

          <Link
            href="/login"
            className={styles.centerLink}
          >
            Back to sign in
          </Link>
        </>
      )}
    </AuthPageShell>
  );
}