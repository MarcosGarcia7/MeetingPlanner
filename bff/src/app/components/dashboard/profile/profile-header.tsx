import Image from 'next/image';
import clsx from 'clsx';
import {CheckBadgeIcon} from '@heroicons/react/24/solid';
import {avatarDataUri} from '@/lib/avatar';
import type {UserProfileResponse} from '@/lib/types/user';
import dashboardStyles from '@/app/components/dashboard/dashboard.module.css';
import styles from './profile.module.css';

export default function ProfileHeader({profile}: { profile: UserProfileResponse }) {
  const fullName = [profile.firstName, profile.lastName].filter(Boolean).join(' ');

  return (
    <header className={styles.header}>
      <Image src={avatarDataUri(profile)} alt='' width={96} height={96} className={dashboardStyles.avatar} unoptimized/>
      <div className={styles.headerText}>
        <h1 className={styles.name}>{fullName || `@${profile.username}`}</h1>
        {fullName && <p className={dashboardStyles.meta}>@{profile.username}</p>}
        <p className={clsx(dashboardStyles.meta, styles.emailLine)}>
          {profile.email}
          {profile.emailVerified && (
            <span className={styles.pill}>
              <CheckBadgeIcon className={styles.pillIcon} aria-hidden/>
              Verified
            </span>
          )}
        </p>
      </div>
    </header>
  );
}
