import type { Metadata } from 'next';

import { ForgotPasswordForm } from '@/features/identity';

export const metadata: Metadata = {
  title: 'Forgot password',
  description: 'Recover access to your Arovia account.',
  robots: {
    index: false,
    follow: false,
  },
};

export default function ForgotPasswordPage() {
  return <ForgotPasswordForm />;
}