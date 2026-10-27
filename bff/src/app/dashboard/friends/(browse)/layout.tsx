import {headers} from 'next/headers';
import {verifySession} from '@/lib/dal';
import {authFetch, getClientIp} from '@/lib/api-client';
import type {CursorPage} from '@/lib/types/api';
import type {FriendshipResponse} from '@/lib/types/friend';
import Tabs from '@/app/components/dashboard/tabs';
import dashboardStyles from '@/app/components/dashboard/dashboard.module.css';
import {incomingCountLabel} from './incoming-count';

export default async function FriendsLayout({children}: { children: React.ReactNode }) {
  await verifySession();
  // The pending count only decorates the tab label; if it fails the pages still load and report errors.
  const incoming = await authFetch<CursorPage<FriendshipResponse>>(
    '/api/users/me/friends/requests/incoming', {}, getClientIp(await headers())
  ).catch(() => null);
  const count = incoming ? incomingCountLabel(incoming) : null;

  const tabs = [
    {label: 'Friends', href: '/dashboard/friends'},
    {label: count ? `Requests (${count})` : 'Requests', href: '/dashboard/friends/requests'},
    {label: 'Blocked', href: '/dashboard/friends/blocked'},
  ];

  return (
    <main className={dashboardStyles.page}>
      <Tabs tabs={tabs} label='Friends'/>
      {children}
    </main>
  );
}
