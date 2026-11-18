import type { Metadata } from 'next';

import { LoginForm } from '@/features/identity';

export const metadata: Metadata = {
  title: 'Sign in',
  description: 'Sign in securely to your Arovia business workspace.',
  robots: {
    index: false,
    follow: false,
  },
};

export default function LoginPage() {
  return <LoginForm />;
}