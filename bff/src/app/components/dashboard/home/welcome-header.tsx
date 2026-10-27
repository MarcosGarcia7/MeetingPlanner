import Image from 'next/image';
import Link from 'next/link';
import {CalendarDaysIcon, UserGroupIcon} from '@heroicons/react/24/solid';
import CountBadge from '@/app/components/dashboard/count-badge';
import type {UserProfileResponse} from '@/lib/types/user';
import {greeting} from './format';
import dashboardStyles from '@/app/components/dashboard/dashboard.module.css';
import styles from './home.module.css';

interface WelcomeHeaderProps {
  username: string;
  profile: UserProfileResponse | null;
  unread: number | null;
}

export default function WelcomeHeader({username, profile, unread}: WelcomeHeaderProps) {
  const timeZone = profile?.timezone ?? 'UTC';
  const name = profile?.firstName || `@${username}`;
  const avatarVersion = new URLSearchParams({
    v: [username, profile?.avatarColor, profile?.firstName, profile?.lastName].join('|'),
  });

  return (
    <header className={styles.header}>
      <Image src={`/api/avatar/me?${avatarVersion}`} alt='' width={64} height={64} className={dashboardStyles.avatar}
             unoptimized/>
      <div className={styles.headerText}>
        <h1 className={styles.greeting}>{greeting(timeZone)}, {name}</h1>
        <p className={dashboardStyles.meta}>
          {unread ? (
            <Link href='/dashboard/inbox' className={styles.inboxLink}>
              <CountBadge count={unread} label='unread notifications'/>
              unread {unread === 1 ? 'notification' : 'notifications'}
            </Link>
          ) : (
            'You’re all caught up.'
          )}
        </p>
      </div>
      <nav className={styles.headerActions} aria-label='Quick actions'>
        <Link href='/dashboard/schedule' className={styles.actionLink}>
          <CalendarDaysIcon className={styles.actionIcon}/>
          New meeting
        </Link>
        <Link href='/dashboard/groups' className={styles.actionLink}>
          <UserGroupIcon className={styles.actionIcon}/>
          New group
        </Link>
      </nav>
    </header>
  );
}
