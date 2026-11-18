'use client';

import { useState, type FormEvent } from 'react';
import Link from 'next/link';

import AuthPageShell from './AuthPageShell';
import styles from './auth.module.css';

import {
  IdentityApiError,
  confirmPasswordReset,
} from '@/features/identity/api/identity-api';

type ResetPasswordFormProps = {
  token: string;
};

export default function ResetPasswordForm({
  token,
}: ResetPasswordFormProps) {
  const [password, setPassword] = useState('');
  const [confirmation, setConfirmation] = useState('');

  const [submitting, setSubmitting] = useState(false);
  const [completed, setCompleted] = useState(false);

  const [error, setError] = useState('');

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    setError('');

    if (!token) {
      setError(
        'This password reset link is incomplete. Request another link.',
      );
      return;
    }

    if (Array.from(password).length < 15) {
      setError(
        'Your new password must contain at least 15 characters.',
      );
      return;
    }

    if (Array.from(password).length > 128) {
      setError(
        'Your password cannot contain more than 128 characters.',
      );
      return;
    }

    if (password !== confirmation) {
      setError('The passwords do not match.');
      return;
    }

    setSubmitting(true);

    try {
      await confirmPasswordReset(token, password);

      setCompleted(true);
      setPassword('');
      setConfirmation('');

    } catch (exception) {
      if (
        exception instanceof IdentityApiError &&
        exception.status === 400
      ) {
        setError(exception.message);
      } else if (
        exception instanceof IdentityApiError &&
        exception.status === 429
      ) {
        setError(
          'Too many reset attempts. Please wait before trying again.',
        );
      } else if (exception instanceof Error) {
        setError(exception.message);
      } else {
        setError(
          'Unable to reset your password. Please try again.',
        );
      }
    } finally {
      setSubmitting(false);
    }
  }

  if (!token) {
    return (
      <AuthPageShell
        eyebrow="ACCOUNT RECOVERY"
        title="Reset link unavailable."
        description="The password reset link does not contain a valid token."
      >
        <div className={styles.errorMessage}>
          Request a new password reset link to continue.
        </div>

        <Link
          href="/forgot-password"
          className={styles.centerLink}
        >
          Request another reset link
        </Link>
      </AuthPageShell>
    );
  }

  if (completed) {
    return (
      <AuthPageShell
        eyebrow="PASSWORD UPDATED"
        title="You're all set."
        description="Your Arovia account password has been changed successfully."
      >
        <div className={styles.successMessage}>
          Your previous password will no longer work.

          <br />
          <br />

          For your security, existing sessions have been revoked.
          Sign in again using your new password.
        </div>

        <Link
          href="/login"
          className={styles.centerLink}
        >
          Continue to sign in
        </Link>
      </AuthPageShell>
    );
  }

  return (
    <AuthPageShell
      eyebrow="ACCOUNT RECOVERY"
      title="Choose a new password."
      description="Create a new password for your Arovia account."
    >
      <form
        className={styles.form}
        onSubmit={handleSubmit}
      >
        <div className={styles.field}>
          <label
            className={styles.label}
            htmlFor="new-password"
          >
            New password
          </label>

          <input
            id="new-password"
            className={styles.input}
            type="password"
            autoComplete="new-password"
            placeholder="Enter your new password"
            value={password}
            onChange={(event) => setPassword(event.target.value)}
            required
            minLength={15}
            maxLength={512}
            disabled={submitting}
          />

          <p className={styles.passwordRequirements}>
            Use at least 15 characters. A longer passphrase is
            recommended.
          </p>
        </div>

        <div className={styles.field}>
          <label
            className={styles.label}
            htmlFor="confirm-password"
          >
            Confirm new password
          </label>

          <input
            id="confirm-password"
            className={styles.input}
            type="password"
            autoComplete="new-password"
            placeholder="Enter your password again"
            value={confirmation}
            onChange={(event) => setConfirmation(event.target.value)}
            required
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
            !password ||
            !confirmation
          }
        >
          {submitting
            ? 'Updating password…'
            : 'Update password'}
        </button>
      </form>

      <Link
        href="/login"
        className={styles.centerLink}
      >
        Back to sign in
      </Link>
    </AuthPageShell>
  );
}