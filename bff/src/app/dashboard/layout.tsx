import {headers} from 'next/headers';
import SideNav from '@/app/components/dashboard/sidenav';
import styles from '@/app/components/dashboard/dashboard.module.css';
import {authFetch, getClientIp} from '@/lib/api-client';
import type {UnreadCountResponse} from '@/lib/types/notification';

// The badge is decorative: any failure (including an expired session, which the page handles)
// just hides it.
async function unreadCount(): Promise<number> {
  try {
    const clientIp = getClientIp(await headers());
    const {unread} = await authFetch<UnreadCountResponse>('/api/users/me/notifications/unread-count', {}, clientIp);
    return unread;
  } catch {
    return 0;
  }
}

export default async function Layout({children}: { children: React.ReactNode }) {
  const unread = await unreadCount();

  return (
    <div className={styles.layoutContainer}>
      <div className={styles.navComponent}>
        <SideNav unread={unread}/>
      </div>
      <div className={styles.mainContent}>
        {children}
      </div>
    </div>
  );
}
