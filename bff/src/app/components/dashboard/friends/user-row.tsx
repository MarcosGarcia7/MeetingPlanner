import Image from 'next/image';
import {avatarDataUri} from '@/lib/avatar';
import {AVATAR_COLORS} from '@/app/components/dashboard/profile/avatar-colors';
import dashboardStyles from '@/app/components/dashboard/dashboard.module.css';
import styles from './friends.module.css';

export interface RowUser {
  username: string;
  firstName: string | null;
  lastName: string | null;
  avatarColor: string | null;
}

interface UserRowProps {
  user: RowUser;
  // Extra inline content after the name, such as a relationship chip.
  badge?: React.ReactNode;
  error?: string | null;
  // The row's actions, rendered on the right.
  children?: React.ReactNode;
}

// One person in a Friends tab list: avatar, full name (or @username), @username, and actions.
export default function UserRow({user, badge, error, children}: UserRowProps) {
  const fullName = [user.firstName, user.lastName].filter(Boolean).join(' ');
  return (
    <li className={styles.row}>
      <Image src={avatarDataUri({
        username: user.username,
        firstName: user.firstName,
        lastName: user.lastName,
        avatarColor: user.avatarColor ?? AVATAR_COLORS[0],
      })} alt='' width={40} height={40} className={dashboardStyles.avatar} unoptimized/>
      <div className={styles.body}>
        <span className={styles.name}>
          {fullName || `@${user.username}`}
          {badge}
        </span>
        {fullName && <span className={dashboardStyles.meta}>@{user.username}</span>}
        {error && <span className={dashboardStyles.error} role='alert'>{error}</span>}
      </div>
      {children}
    </li>
  );
}
