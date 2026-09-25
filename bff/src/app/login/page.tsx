import styles from '@/app/ui/auth/auth.module.css';
import LoginForm from '@/app/ui/auth/login/login-form';

export default function LoginPage() {
  return (
    <main className={styles.page}>
        <div className={styles.card}>
        <LoginForm />
        </div>
    </main>
  );
}