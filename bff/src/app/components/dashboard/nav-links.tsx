'use client';

import Link from 'next/link';
import {
  HomeIcon,
  UserGroupIcon,
  CalendarDaysIcon,
  EnvelopeIcon,
  UsersIcon,
  CodeBracketIcon,
  Cog8ToothIcon,
  UserIcon
} from '@heroicons/react/24/solid';
import {usePathname} from 'next/navigation';
import {useEffect, useState} from 'react';
import type {UnreadCountResponse} from '@/lib/types/notification';
import {sendJson} from '@/lib/send-json';
import CountBadge from '@/app/components/dashboard/count-badge';
import styles from './sidenav.module.css';
import clsx from 'clsx';

const links = [
  {
    name: 'Home',
    href: '/dashboard',
    icon: HomeIcon
  },
  {
    name: 'Profile',
    href: '/dashboard/profile',
    icon: UserIcon
  },
  {
    name: 'Groups',
    href: '/dashboard/groups',
    icon: UserGroupIcon
  },
  {
    name: 'Schedule',
    href: '/dashboard/schedule',
    icon: CalendarDaysIcon
  },
  {
    name: 'Inbox',
    href: '/dashboard/inbox',
    icon: EnvelopeIcon
  },
  {
    name: 'Friends',
    href: '/dashboard/friends',
    icon: UsersIcon
  },
  {
    name: 'block',
    href: '',
    icon: CodeBracketIcon
  },
  {
    name: 'Settings',
    href: '/dashboard/settings',
    icon: Cog8ToothIcon
  }
]

// Home matches only itself; other sections stay highlighted on their sub-pages (e.g. /dashboard/groups/mine).
function isCurrent(pathname: string, href: string) {
  return pathname === href || (href !== '/dashboard' && pathname.startsWith(`${href}/`));
}

// The layout's server-rendered count only changes on router.refresh(); layouts don't re-render on navigation.
// So the badge also refetches on every page change and whenever the tab regains focus. No timer: every /api
// call slides the session's idle TTL, and polling would keep an abandoned tab signed in.
function useUnreadCount(initial: number, pathname: string) {
  const [count, setCount] = useState(initial);
  const [lastInitial, setLastInitial] = useState(initial);
  if (initial !== lastInitial) {
    setLastInitial(initial);
    setCount(initial);
  }

  useEffect(() => {
    let cancelled = false;
    async function refresh() {
      const result = await sendJson<UnreadCountResponse>('/api/users/me/notifications/unread-count');
      if (!cancelled && result.ok) {
        setCount(result.data.unread);
      }
    }
    function onVisible() {
      if (document.visibilityState === 'visible') {
        void refresh();
      }
    }
    void refresh();
    document.addEventListener('visibilitychange', onVisible);
    return () => {
      cancelled = true;
      document.removeEventListener('visibilitychange', onVisible);
    };
  }, [pathname]);

  return count;
}

export default function NavLinks({unread: initialUnread}: { unread: number }) {
  const pathname = usePathname();
  const unread = useUnreadCount(initialUnread, pathname);

  return (
    <>
      {links.map((link) => {
        if (link.name === 'block') {
          return (<div key={link.name} className={styles.sidenavBlock}></div>);
        } else {
          const LinkIcon = link.icon;
          return (
            <Link
              key={link.name}
              href={link.href}
              className={clsx(styles.navLink, {[styles.currentNavLink]: isCurrent(pathname, link.href)})}
            >
              <LinkIcon className={styles.linkIcon}/>
              <p className={styles.name}>{link.name}</p>
              {link.href === '/dashboard/inbox' && (
                <span className={styles.navBadge}>
                  <CountBadge count={unread} label='unread notifications'/>
                </span>
              )}
            </Link>
          );
        }
      })}
    </>
  );
}
