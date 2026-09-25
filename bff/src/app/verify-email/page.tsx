import { Suspense } from 'react';
import Image from 'next/image';
import styles from '@/app/ui/auth/auth.module.css';
import VerifyEmailContent from '../ui/verify-email/verify-email-content';

export default function VerifyEmailPage() {
  return (
    <main className={styles.page}>
      <div className={styles.card}>
        <Image
          className={styles.logo}
          src='/logo-mp.svg'
          alt='Meeting Planner logo'
          width={64}
          height={64}
          priority
        />
        <h1 className={styles.title}>Verify your email</h1>
        <p className={styles.subtitle}>
          Confirm this link belongs to you by verifying your email address.
        </p>
        <Suspense fallback={<p className={styles.subtitle}>Verifying...</p>}>
          <VerifyEmailContent />
        </Suspense>
      </div>
    </main>
  );
}