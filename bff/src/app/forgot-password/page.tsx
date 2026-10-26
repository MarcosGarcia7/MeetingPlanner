import styles from '@/app/components/auth/auth.module.css';
import ForgotPasswordForm from '@/app/components/auth/forgot-password/forgot-password-form';

export default function ForgotPasswordPage() {
  return (
    <main className={styles.page}>
      <div className={styles.card}>
        <ForgotPasswordForm/>
      </div>
    </main>
  );
}
