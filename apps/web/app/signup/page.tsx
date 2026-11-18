
import type { Metadata } from 'next';

import { RegistrationForm } from '@/features/identity';

export const metadata: Metadata = {
  title: 'Create your account',
  description: 'Create your Arovia account.',
  robots: {
    index: false,
    follow: false,
  },
};

export default function SignupPage() {
  return <RegistrationForm />;
}
