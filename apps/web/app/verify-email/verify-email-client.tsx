
'use client';

import {
  useEffect,
  useRef,
  useState,
  type CSSProperties,
} from 'react';
import Link from 'next/link';
import { useSearchParams } from 'next/navigation';

type VerificationState =
  | 'ready'
  | 'verifying'
  | 'verified'
  | 'invalid'
  | 'missing'
  | 'failed';

const API_BASE_URL = (
  process.env.NEXT_PUBLIC_API_BASE_URL ?? 'http://localhost:8080'
).replace(/\/+$/, '');

const stateContent: Record<
  VerificationState,
  { title: string; description: string; symbol: string }
> = {
  ready: {
    title: 'Verify your email address',
    description:
      'Click below to confirm that you own this email address and activate your Arovia account.',
    symbol: '✉',
  },
  verifying: {
    title: 'Verifying your email',
    description: 'We are securely confirming your email address.',
    symbol: '…',
  },
  verified: {
    title: 'Email verified',
    description:
      'Your email address is verified and your Arovia account is now active.',
    symbol: '✓',
  },
  invalid: {
    title: 'Verification link unavailable',
    description:
      'This link is invalid, expired, or has already been used.',
    symbol: '!',
  },
  missing: {
    title: 'Verification link incomplete',
    description:
      'We could not find a verification token in this link.',
    symbol: '!',
  },
  failed: {
    title: 'Verification could not be completed',
    description:
      'We could not reach Arovia. Check your connection and try again.',
    symbol: '!',
  },
};

export default function VerifyEmailClient() {
  const searchParams = useSearchParams();
  const token = searchParams.get('token');

  const [status, setStatus] = useState<VerificationState | null>(null);

  // Only accessed from the event handler, never during rendering.
  const submitting = useRef(false);

  /*
   * Synchronize browser history with the page.
   * The token remains available through the searchParams value
   * captured by this component.
   */
  useEffect(() => {
    if (token) {
      window.history.replaceState(
        window.history.state,
        '',
        '/verify-email',
      );
    }
  }, [token]);

  /*
   * Derive the initial UI state instead of synchronously
   * updating React state inside an effect.
   */
  const currentStatus: VerificationState =
    status ?? (token ? 'ready' : 'missing');

  const verifyEmail = async () => {
    if (!token || submitting.current) {
      return;
    }

    submitting.current = true;
    setStatus('verifying');

    try {
      const response = await fetch(
        `${API_BASE_URL}/api/v1/identity/email-verification/confirm`,
        {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
          },
          body: JSON.stringify({ token }),
          cache: 'no-store',
        },
      );

      if (response.status === 204) {
        setStatus('verified');
      } else if (response.status === 400) {
        setStatus('invalid');
      } else {
        setStatus('failed');
      }
    } catch {
      setStatus('failed');
    } finally {
      submitting.current = false;
    }
  };

  const content = stateContent[currentStatus];

  const successful = currentStatus === 'verified';
  const working = currentStatus === 'verifying';
  const canVerify = currentStatus === 'ready';
  const canRetry =
    currentStatus === 'failed' && Boolean(token);

  return (
    <main
      style={{
        minHeight: '100vh',
        display: 'grid',
        placeItems: 'center',
        padding: '24px',
        background:
          'radial-gradient(circle at 50% 20%, #eef6f3 0, #f7f8fa 48%, #f7f8fa 100%)',
      }}
    >
      <section
        aria-live="polite"
        aria-busy={working}
        style={{
          width: '100%',
          maxWidth: '460px',
          padding: '42px 36px',
          border: '1px solid #e9edf1',
          borderRadius: '16px',
          background: '#ffffff',
          boxShadow: '0 18px 60px rgba(28, 42, 56, 0.08)',
          textAlign: 'center',
        }}
      >
        <div
          style={{
            display: 'inline-flex',
            alignItems: 'center',
            gap: '9px',
            marginBottom: '36px',
          }}
        >
          <div
            style={{
              width: '32px',
              height: '32px',
              display: 'grid',
              placeItems: 'center',
              borderRadius: '9px',
              background: '#176b61',
              color: '#ffffff',
              fontSize: '18px',
              fontWeight: 800,
              transform: 'rotate(-4deg)',
            }}
          >
            A
          </div>

          <span
            style={{
              color: '#17212b',
              fontSize: '23px',
              fontWeight: 800,
              letterSpacing: '-1px',
            }}
          >
            arovia
          </span>
        </div>

        <div
          aria-hidden="true"
          style={{
            width: '58px',
            height: '58px',
            display: 'grid',
            placeItems: 'center',
            margin: '0 auto 22px',
            borderRadius: '50%',
            background: successful ? '#e8f3f0' : '#f1f4f5',
            color: successful ? '#176b61' : '#71808b',
            fontSize: '27px',
            fontWeight: 700,
          }}
        >
          {content.symbol}
        </div>

        <p
          style={{
            margin: '0 0 9px',
            color: '#8b969f',
            fontSize: '10px',
            fontWeight: 700,
            letterSpacing: '1.4px',
            textTransform: 'uppercase',
          }}
        >
          Arovia account verification
        </p>

        <h1
          style={{
            margin: '0 0 12px',
            color: '#202d38',
            fontSize: '25px',
            fontWeight: 800,
            letterSpacing: '-0.8px',
          }}
        >
          {content.title}
        </h1>

        <p
          style={{
            maxWidth: '340px',
            margin: '0 auto 27px',
            color: '#788590',
            fontSize: '13px',
            lineHeight: 1.7,
          }}
        >
          {content.description}
        </p>

        {(canVerify || canRetry) && (
          <button
            type="button"
            onClick={verifyEmail}
            style={primaryButtonStyle}
          >
            {canRetry ? 'Try again' : 'Verify email address'}
          </button>
        )}

        {!working && !canVerify && !canRetry && (
          <Link href="/" style={primaryLinkStyle}>
            Return to Arovia
          </Link>
        )}

        <p
          style={{
            margin: '34px 0 0',
            paddingTop: '18px',
            borderTop: '1px solid #eef0f2',
            color: '#a0a9b1',
            fontSize: '10px',
          }}
        >
          Secure account access, built around your business.
        </p>
      </section>
    </main>
  );
}

const primaryButtonStyle: CSSProperties = {
  minHeight: '42px',
  padding: '0 19px',
  border: 0,
  borderRadius: '8px',
  background: '#176b61',
  color: '#ffffff',
  fontSize: '12px',
  fontWeight: 700,
  cursor: 'pointer',
};

const primaryLinkStyle: CSSProperties = {
  minHeight: '42px',
  display: 'inline-flex',
  alignItems: 'center',
  justifyContent: 'center',
  padding: '0 19px',
  borderRadius: '8px',
  background: '#176b61',
  color: '#ffffff',
  fontSize: '12px',
  fontWeight: 700,
  textDecoration: 'none',
};
