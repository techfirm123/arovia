
'use client';

import {
  useCallback,
  useEffect,
  useState,
  type ReactNode,
} from 'react';

import { usePathname, useRouter } from 'next/navigation';

import {
  fetchCurrentIdentity,
  IdentityApiError,
  logoutCurrentSession,
  type IdentityUser,
} from '@/features/identity/api/identity-api';

import styles from './auth.module.css';

type IdentityBoundaryProps = {
  children: ReactNode;
};

const PUBLIC_IDENTITY_ROUTES = [
  '/',
  '/signup',
  '/login',
  '/forgot-password',
  '/reset-password',
  '/verify-email',
] as const;

function isPublicRoute(pathname: string): boolean {
  if (pathname === '/') {
    return true;
  }

  return PUBLIC_IDENTITY_ROUTES.some(
    (route) =>
      route !== '/' &&
      (pathname === route ||
        pathname.startsWith(`${route}/`)),
  );
}

export default function IdentityBoundary({
  children,
}: IdentityBoundaryProps) {
  const pathname = usePathname();
  const router = useRouter();

  const [identity, setIdentity] = useState<IdentityUser | null>(null);
  const [checkedPath, setCheckedPath] = useState<string | null>(null);

  const [error, setError] = useState('');
  const [signOutError, setSignOutError] = useState('');

  const [signingOut, setSigningOut] = useState(false);
  const [attempt, setAttempt] = useState(0);

  useEffect(() => {
    if (isPublicRoute(pathname)) {
      return;
    }

    let cancelled = false;

    fetchCurrentIdentity()
      .then((currentIdentity) => {
        if (cancelled) {
          return;
        }

        setIdentity(currentIdentity);
        setError('');
        setCheckedPath(pathname);
      })
      .catch((exception: unknown) => {
        if (cancelled) {
          return;
        }

        if (
          exception instanceof IdentityApiError &&
          exception.status === 401
        ) {
          router.replace('/login');
          return;
        }

        setIdentity(null);

        setError(
          exception instanceof Error
            ? exception.message
            : 'Unable to verify your session.',
        );

        setCheckedPath(pathname);
      });

    return () => {
      cancelled = true;
    };
  }, [pathname, router, attempt]);

  const handleLogout = useCallback(async () => {
    setSigningOut(true);
    setSignOutError('');

    try {
      await logoutCurrentSession();

      setIdentity(null);

      router.replace('/');
      router.refresh();
    } catch (exception) {
      if (
        exception instanceof IdentityApiError &&
        exception.status === 401
      ) {
        router.replace('/');
        return;
      }

      setSignOutError(
        exception instanceof Error
          ? exception.message
          : 'Unable to sign out.',
      );
    } finally {
      setSigningOut(false);
    }
  }, [router]);

  if (isPublicRoute(pathname)) {
    return <>{children}</>;
  }

  if (checkedPath !== pathname) {
    return (
      <main className={styles.boundaryLoading}>
        <span>Checking your Arovia session…</span>
      </main>
    );
  }

  if (error || !identity) {
    return (
      <main className={styles.boundaryError}>
        <h2>Unable to verify your session</h2>

        <p>
          {error || 'Your session could not be verified.'}
        </p>

        <button
          type="button"
          className={styles.primaryButton}
          onClick={() => {
            setError('');
            setCheckedPath(null);
            setAttempt((current) => current + 1);
          }}
        >
          Try again
        </button>

        <button
          type="button"
          className={styles.centerLink}
          onClick={() => router.replace('/login')}
        >
          Return to sign in
        </button>
      </main>
    );
  }

  return (
    <>
      {children}

      <aside className={styles.sessionDock}>
        <div className={styles.sessionIdentity}>
          <strong>{identity.displayName}</strong>
          <span>{identity.email}</span>
        </div>

        <button
          type="button"
          className={styles.signOutButton}
          onClick={handleLogout}
          disabled={signingOut}
        >
          {signingOut ? 'Signing out…' : 'Sign out'}
        </button>

        {signOutError && (
          <span role="alert" className={styles.errorMessage}>
            {signOutError}
          </span>
        )}
      </aside>
    </>
  );
}
