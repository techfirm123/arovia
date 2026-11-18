import type { Metadata, Viewport } from 'next';

import './globals.css';

import { IdentityBoundary } from '@/features/identity';

export const metadata: Metadata = {
  title: {
    default: 'Arovia — Computer Business OS',
    template: '%s | Arovia',
  },
  description:
    'The AI-powered operating platform for computer sales and service businesses.',
  applicationName: 'Arovia',
};

export const viewport: Viewport = {
  width: 'device-width',
  initialScale: 1,
  themeColor: '#f7f8fa',
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="en">
      <body>
        <IdentityBoundary>
          {children}
        </IdentityBoundary>
      </body>
    </html>
  );
}