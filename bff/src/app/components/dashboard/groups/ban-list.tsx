'use client';

import {useState} from 'react';
import {useRouter} from 'next/navigation';
import type {CursorPage} from '@/lib/types/api';
import type {GroupBanResponse} from '@/lib/types/group';
import {sendJson} from '@/lib/send-json';
import {useCursorList} from '@/app/components/dashboard/use-cursor-list';
import {formatDateTime} from '@/app/components/dashboard/home/format';
import type {GroupRole} from './load-group';
import dashboardStyles from '@/app/components/dashboard/dashboard.module.css';
import styles from './groups.module.css';

interface BanListProps {
  groupId: number;
  initial: CursorPage<GroupBanResponse> | null;
  role: GroupRole;
  timeZone: string;
}

// Banned users with unban. Bans the owner made can only be lifted by the owner.
export default function BanList({groupId, initial, role, timeZone}: BanListProps) {
  const router = useRouter();
  const bans = useCursorList(`/api/groups/${groupId}/bans`, initial, 'Couldn’t load bans.');
  const [pending, setPending] = useState<number | null>(null);
  const [error, setError] = useState<{ userId: number; message: string } | null>(null);

  async function unban(userId: number) {
    setPending(userId);
    setError(null);
    const result = await sendJson(`/api/groups/${groupId}/bans/${userId}`, 'DELETE');
    setPending(null);
    if (!result.ok) {
      if (result.unauthenticated) {
        router.push('/login');
        return;
      }
      setError({userId, message: result.message});
      return;
    }
    bans.setItems((current) => current.filter((ban) => ban.userId !== userId));
  }

  return (
    <section className={dashboardStyles.card} aria-labelledby='bans-title'>
      <div className={dashboardStyles.cardHeader}>
        <h2 id='bans-title' className={dashboardStyles.cardTitle}>Banned</h2>
      </div>
      {bans.loaded && bans.items.length === 0 ? (
        <p className={dashboardStyles.empty}>No one is banned.</p>
      ) : (
        <ul className={styles.groupList}>
          {bans.items.map((ban) => {
            const ownerOnly = ban.bannedByOwner && role !== 'OWNER';
            return (
              <li key={ban.userId} className={styles.groupCard}>
                <div className={styles.groupBody}>
                  <span className={styles.groupName}>@{ban.username}</span>
                  <span className={dashboardStyles.meta}>
                    Banned by @{ban.bannedByUsername}
                    {ban.bannedAt && <> · {formatDateTime(ban.bannedAt, timeZone)}</>}
                  </span>
                  {ownerOnly && <span className={dashboardStyles.meta}>Only the owner can lift this ban.</span>}
                  {error?.userId === ban.userId && <span className={dashboardStyles.error}>{error.message}</span>}
                </div>
                {!ownerOnly && (
                  <div className={dashboardStyles.actions}>
                    <button type='button' className={dashboardStyles.buttonSecondary}
                            disabled={pending === ban.userId} onClick={() => unban(ban.userId!)}>
                      Unban
                    </button>
                  </div>
                )}
              </li>
            );
          })}
        </ul>
      )}
      {bans.error && <p className={dashboardStyles.error}>{bans.error}</p>}
      {bans.hasMore && (
        <div className={styles.loadMore}>
          <button type='button' className={dashboardStyles.buttonSecondary} disabled={bans.loading}
                  onClick={bans.loadMore}>
            {bans.loading ? 'Loading…' : 'Load more'}
          </button>
        </div>
      )}
    </section>
  );
}
