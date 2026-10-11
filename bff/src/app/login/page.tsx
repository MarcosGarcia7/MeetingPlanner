import styles from '@/app/components/auth/auth.module.css';
import LoginForm from '@/app/components/auth/login/login-form';

export default function LoginPage() {
  return (
    <main className={styles.page}>
        <div className={styles.card}>
        <LoginForm />
        </div>
    </main>
  );
}