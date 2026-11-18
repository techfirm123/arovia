
'use client';

import { useState, type FormEvent } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';

import AuthPageShell from './AuthPageShell';
import styles from './auth.module.css';

import {
  IdentityApiError,
  loginIdentity,
} from '@/features/identity/api/identity-api';

export default function LoginForm() {
  const router = useRouter();

  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');

  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    setError('');
    setSubmitting(true);

    try {
      await loginIdentity(email.trim(), password);

      router.replace('/app');
      router.refresh();
    } catch (exception) {
      if (
        exception instanceof IdentityApiError &&
        exception.status === 401
      ) {
        setError(
          'The email address or password is incorrect. ' +
            'Please check your details and try again.',
        );
      } else if (
        exception instanceof IdentityApiError &&
        exception.status === 429
      ) {
        setError(
          'Too many sign-in attempts. Please wait before trying again.',
        );
      } else if (exception instanceof Error) {
        setError(exception.message);
      } else {
        setError('Unable to sign in. Please try again.');
      }
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <AuthPageShell
      eyebrow="WELCOME BACK"
      title="Sign in to Arovia."
      description="Access your business workspace securely."
      footer={
        <Link
          href="/forgot-password"
          className={styles.textLink}
        >
          Forgot your password?
        </Link>
      }
    >
      <form
        className={styles.form}
        onSubmit={handleSubmit}
      >
        <div className={styles.field}>
          <label
            className={styles.label}
            htmlFor="login-email"
          >
            Email address
          </label>

          <input
            id="login-email"
            className={styles.input}
            type="email"
            autoComplete="username"
            placeholder="you@company.com"
            value={email}
            onChange={(event) => setEmail(event.target.value)}
            required
            maxLength={254}
            disabled={submitting}
          />
        </div>

        <div className={styles.field}>
          <label
            className={styles.label}
            htmlFor="login-password"
          >
            Password
          </label>

          <input
            id="login-password"
            className={styles.input}
            type="password"
            autoComplete="current-password"
            placeholder="Enter your password"
            value={password}
            onChange={(event) => setPassword(event.target.value)}
            required
            disabled={submitting}
          />
        </div>

        {error && (
          <div
            className={styles.errorMessage}
            role="alert"
          >
            {error}
          </div>
        )}

        <button
          className={styles.primaryButton}
          type="submit"
          disabled={
            submitting ||
            !email.trim() ||
            !password
          }
        >
          {submitting ? 'Signing in…' : 'Sign in'}
        </button>
      </form>
    </AuthPageShell>
  );
}
