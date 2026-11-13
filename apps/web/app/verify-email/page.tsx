
import type { Metadata } from 'next';
import { Suspense } from 'react';
import VerifyEmailClient from './verify-email-client';

export const metadata: Metadata = {
  title: 'Verify your email',
  description: 'Verify your email address to activate your Arovia account.',
  referrer: 'no-referrer',
  robots: {
    index: false,
    follow: false,
  },
};

function VerificationPageFallback() {
  return (
    <main
      style={{
        minHeight: '100vh',
        display: 'grid',
        placeItems: 'center',
        padding: '24px',
        background: '#f7f8fa',
      }}
    >
      <p style={{ color: '#788590', fontSize: '13px' }}>
        Preparing your verification page...
      </p>
    </main>
  );
}

export default function VerifyEmailPage() {
  return (
    <Suspense fallback={<VerificationPageFallback />}>
      <VerifyEmailClient />
    </Suspense>
  );
}
