import Image from 'next/image';
import styles from '@/app/components/auth/auth.module.css';
import SignupForm from '@/app/components/auth/signup/signup-form';

export default function SignupPage() {
  return (
    <main className={styles.page}>
      <div className={styles.card}>
        <SignupForm/>
      </div>
    </main>
  );
}
