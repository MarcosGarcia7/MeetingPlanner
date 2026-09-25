import Image from 'next/image';
import styles from '@/app/ui/auth/auth.module.css';
import SignupForm from '@/app/ui/auth/signup/signup-form';

export default function SignupPage() {
  return (
    <main className={styles.page}>
      <div className={styles.card}>
        <SignupForm />
      </div>
    </main>
  );
}