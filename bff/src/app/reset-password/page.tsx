import {Suspense} from 'react';
import type {Metadata} from 'next';
import Image from 'next/image';
import styles from '@/app/components/auth/auth.module.css';
import ResetPasswordForm from '@/app/components/auth/reset-password/reset-password-form';

// The reset token is in the URL, so never leak it to other sites through the Referer header.
export const metadata: Metadata = {
  referrer: 'no-referrer',
};

export default function ResetPasswordPage() {
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
        <Suspense fallback={<p className={styles.subtitle}>Loading...</p>}>
          <ResetPasswordForm/>
        </Suspense>
      </div>
    </main>
  );
}
