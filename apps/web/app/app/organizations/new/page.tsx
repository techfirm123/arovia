
import type { Metadata } from 'next';
import Link from 'next/link';
import { ChevronRight } from 'lucide-react';

import { OrganizationOnboardingForm } from '@/features/organization';

import styles from './page.module.css';

export const metadata: Metadata = {
  title: 'Create your business workspace',
};

export default function NewOrganizationPage() {
  return (
    <main className={styles.page}>
      <div className={styles.breadcrumb}>
        <Link href="/app">Workspace</Link>
        <ChevronRight size={13} />
        <span>Create organization</span>
      </div>

      <header className={styles.heading}>
        <div className={styles.eyebrow}>GET STARTED WITH AROVIA</div>
        <h1>Set up your business<span>.</span></h1>
        <p>
          Tell us a little about your business. You can configure your
          team and operational workflows after creating your workspace.
        </p>
      </header>

      <OrganizationOnboardingForm />
    </main>
  );
}
