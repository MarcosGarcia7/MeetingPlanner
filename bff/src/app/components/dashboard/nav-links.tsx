'use client';

import Link from 'next/link';
import {
  HomeIcon,
  UserGroupIcon,
  CalendarDaysIcon,
  EnvelopeIcon,
  UsersIcon,
  PlusCircleIcon,
  CodeBracketIcon,
  Cog8ToothIcon,
  UserIcon
} from '@heroicons/react/24/solid';
import {usePathname} from 'next/navigation';
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

export default function NavLinks({unread}: { unread: number }) {
  const pathname = usePathname();

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
              className={clsx(styles.navLink, {[styles.currentNavLink]: pathname === link.href})}
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
