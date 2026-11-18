'use client';

import { useEffect } from 'react';
import { useSearchParams } from 'next/navigation';

import ResetPasswordForm from './auth/ResetPasswordForm';

export default function ResetPasswordClient() {
  const searchParams = useSearchParams();
  const token = searchParams.get('token') ?? '';

  useEffect(() => {
    if (!token) {
      return;
    }

    window.history.replaceState(
      window.history.state,
      '',
      '/reset-password',
    );
  }, [token]);

  return <ResetPasswordForm token={token} />;
}