import type { Metadata } from 'next';
import { Suspense } from 'react';

import { ResetPasswordClient } from '@/features/identity';

export const metadata: Metadata = {
  title: 'Reset password',
  description: 'Choose a new password for your Arovia account.',
  referrer: 'no-referrer',
  robots: {
    index: false,
    follow: false,
  },
};

function ResetPasswordFallback() {
  return (
    <main
      style={{
        minHeight: '100vh',
        display: 'grid',
        placeItems: 'center',
        background: '#f7f8fa',
        color: '#788590',
        fontSize: '12px',
      }}
    >
      Preparing your password reset page…
    </main>
  );
}

export default function ResetPasswordPage() {
  return (
    <Suspense fallback={<ResetPasswordFallback />}>
      <ResetPasswordClient />
    </Suspense>
  );
}