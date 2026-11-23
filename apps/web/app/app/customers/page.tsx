import type { Metadata } from 'next';
import Link from 'next/link';

import { OrganizationSwitcher } from '@/features/organization';
import { CustomersWorkspace } from '@/features/customer';

export const metadata: Metadata = {
  title: 'Customers',
};

export default function CustomersPage() {
  return (
    <main className="app-shell">
      <aside
        style={{
          width: 254,
          position: 'fixed',
          inset: '0 auto 0 0',
          zIndex: 30,
          display: 'flex',
          flexDirection: 'column',
          gap: 22,
          padding: '23px 15px 14px',
          borderRight: '1px solid var(--border)',
          background: '#fff',
        }}
      >
        <Link
          href="/app"
          style={{
            display: 'flex',
            alignItems: 'center',
            gap: 10,
            padding: '0 9px',
            color: 'var(--text)',
            textDecoration: 'none',
            fontSize: 20,
            fontWeight: 800,
            letterSpacing: '-1px',
          }}
        >
          arovia
        </Link>

        <OrganizationSwitcher />

        <nav aria-label="Workspace navigation">
          <Link
            href="/app"
            style={{
              display: 'block',
              padding: '10px',
              borderRadius: 7,
              color: '#697683',
              textDecoration: 'none',
              fontSize: 12,
            }}
          >
            ← Overview
          </Link>

          <div
            aria-current="page"
            style={{
              marginTop: 4,
              padding: '10px',
              borderRadius: 7,
              background: 'var(--brand-soft)',
              color: 'var(--brand-dark)',
              fontSize: 12,
              fontWeight: 700,
            }}
          >
            Customers
          </div>
        </nav>

        <div
          style={{
            marginTop: 'auto',
            padding: '12px 9px',
            borderTop: '1px solid var(--border)',
            color: 'var(--text-muted)',
            fontSize: 9,
          }}
        >
          AROVIA · BUSINESS OS
        </div>
      </aside>

      <section
        className="main-panel"
        style={{ marginLeft: 254 }}
      >
        <header
          style={{
            height: 67,
            display: 'flex',
            alignItems: 'center',
            padding: '0 37px',
            borderBottom: '1px solid var(--border)',
            background: 'rgba(255,255,255,.91)',
          }}
        >
          <div className="breadcrumb">
            <Link
              href="/app"
              style={{ color: 'inherit', textDecoration: 'none' }}
            >
              Workspace
            </Link>
            <span>›</span>
            <strong>Customers</strong>
          </div>
        </header>

        <div className="page-content">
          <CustomersWorkspace />
          <footer className="page-footer">
            <span>AROVIA · BUSINESS OS</span>
            <span>Built around the way your business works.</span>
          </footer>
        </div>
      </section>
    </main>
  );
}