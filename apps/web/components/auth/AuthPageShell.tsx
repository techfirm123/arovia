import type { ReactNode } from 'react';
import styles from './auth.module.css';

type AuthPageShellProps = {
  eyebrow: string;
  title: string;
  description: string;
  children: ReactNode;
  footer?: ReactNode;
};

export default function AuthPageShell({
  eyebrow,
  title,
  description,
  children,
  footer,
}: AuthPageShellProps) {
  return (
    <main className={styles.authPage}>
      <div className={styles.authGlow} aria-hidden="true" />

      <div className={styles.authLayout}>
        <div className={styles.brandLockup}>
          <div className={styles.brandMark} aria-hidden="true">
            <span />
            <span />
            <span />
          </div>

          <div>
            <div className={styles.brandName}>arovia</div>
            <div className={styles.brandCaption}>
              BUSINESS OS
            </div>
          </div>
        </div>

        <section className={styles.authCard}>
          <p className={styles.eyebrow}>{eyebrow}</p>

          <h1 className={styles.title}>{title}</h1>

          <p className={styles.description}>
            {description}
          </p>

          {children}

          {footer && (
            <div className={styles.formFooter}>
              {footer}
            </div>
          )}
        </section>

        <p className={styles.authFooter}>
          Secure access to your business workspace.
          <br />
          Arovia · Computer Business OS
        </p>
      </div>
    </main>
  );
}