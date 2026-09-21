import Image from 'next/image';
import styles from '../ui/login/login.module.css';
import LoginForm from '../ui/login/login-form';

export default function LoginPage() {
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
        <h1 className={styles.title}>Log in</h1>
        <p className={styles.subtitle}>Enter your credentials to continue.</p>
        <LoginForm />
      </div>
    </main>
  );
}