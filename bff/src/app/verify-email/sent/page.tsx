import Image from 'next/image';
import Link from 'next/link';
import styles from '@/app/ui/auth/auth.module.css';

export default async function VerificationSentPage({
  searchParams,
}: {
  searchParams: Promise<{ [key: string]: string | string[] | undefined }>;
}) {
  const { email } = await searchParams;
  const emailValue = typeof email === 'string' ? email : null;

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
        <h1 className={styles.title}>Check your inbox</h1>
        <p className={styles.subtitle}>
          We sent a verification link
          {emailValue ? (
            <>
              {' '}
              to <strong>{emailValue}</strong>
            </>
          ) : (
            ''
          )}
          . Click it to verify your email, then sign in to your account.
        </p>
        <Link
          className={styles.submit}
          href='/login'
          style={{ textAlign: 'center', display: 'block', marginTop: 16 }}
        >
          Go to login
        </Link>
      </div>
    </main>
  );
}