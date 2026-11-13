import styles from '@/app/components/auth/auth.module.css';
import LoginForm from '@/app/components/auth/login/login-form';
import {safeNextPath} from '@/lib/next-path';

export default async function LoginPage({searchParams}: PageProps<'/login'>) {
  const {next} = await searchParams;

  return (
    <main className={styles.page}>
      <div className={styles.card}>
        <LoginForm next={safeNextPath(next)}/>
      </div>
    </main>
  );
}
