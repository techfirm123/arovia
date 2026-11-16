import type { Metadata } from 'next';

import ForgotPasswordForm from '@/components/auth/ForgotPasswordForm';

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