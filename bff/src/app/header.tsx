import Image from 'next/image';
import Link from 'next/link';
import { getSession } from '@/lib/session';
import LogoutButton from './logout-button';
import styles from './header.module.css';

export default async function Header() {
  const session = await getSession();

  return (
    <header className={styles.header}>
      <Link href='/' className={styles.logoLink} aria-label='Meeting Planner home'>
        <Image
          className={styles.logo}
          src='/logo-mp.svg'
          alt='Meeting Planner logo'
          width={32}
          height={32}
          priority
        />
      </Link>
      <h1 className={styles.h1}>Meeting Planner</h1>
      <nav className={styles.nav}>
        {session ? (
          <LogoutButton />
        ) : (
          <>
            <Link className={styles.buttonSecondary} href='/login'>
              login
            </Link>
            <Link className={styles.buttonPrimary} href='/signup'>
              sign up
            </Link>
          </>
        )}
      </nav>
    </header>
  );
}