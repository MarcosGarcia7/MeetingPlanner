'use client';

import {useState} from 'react';
import {useRouter} from 'next/navigation';
import type {CursorPage} from '@/lib/types/api';
import type {GroupSummaryResponse} from '@/lib/types/group';
import {readApiError} from '@/lib/api-error';
import CreateGroupForm from './create-group-form';
import GroupCard from './group-card';
import dashboardStyles from '@/app/components/dashboard/dashboard.module.css';
import styles from './groups.module.css';

interface MyGroupsProps {
  // First page from the server render, or null when that request failed.
  initial: CursorPage<GroupSummaryResponse> | null;
  viewerId: number;
  // Open the create form on load (linked as ?new=1).
  creating: boolean;
}

// The My groups tab: every group the user is in, a page at a time, plus the create form.
export default function MyGroups({initial, viewerId, creating: initiallyCreating}: MyGroupsProps) {
  const router = useRouter();
  const [creating, setCreating] = useState(initiallyCreating);
  const [groups, setGroups] = useState(initial?.items ?? []);
  const [cursor, setCursor] = useState(initial?.nextCursor ?? null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(initial === null ? 'Couldn’t load your groups.' : null);

  async function loadMore() {
    if (!cursor) {
      return;
    }
    setLoading(true);
    setError(null);
    try {
      const response = await fetch(`/api/groups?cursor=${encodeURIComponent(cursor)}`);
      if (!response.ok) {
        const apiError = await readApiError(response);
        if (apiError?.error === 'unauthenticated') {
          router.push('/login');
          return;
        }
        setError(apiError?.message ?? 'Couldn’t load more groups. Please try again.');
        return;
      }
      const page = (await response.json()) as CursorPage<GroupSummaryResponse>;
      setGroups((current) => [...current, ...page.items]);
      setCursor(page.nextCursor);
    } catch {
      setError('Couldn’t reach the server. Please try again.');
    } finally {
      setLoading(false);
    }
  }

  return (
    <>
      {creating && (
        <section className={dashboardStyles.card}>
          <div className={dashboardStyles.cardHeader}>
            <h2 className={dashboardStyles.cardTitle}>New group</h2>
          </div>
          <CreateGroupForm onCancel={() => setCreating(false)}/>
        </section>
      )}
      <section className={dashboardStyles.card}>
        <div className={styles.listHeader}>
          <h2 className={dashboardStyles.cardTitle}>My groups</h2>
          {!creating && (
            <button type='button' className={dashboardStyles.button} onClick={() => setCreating(true)}>
              New group
            </button>
          )}
        </div>
        {initial !== null && groups.length === 0 ? (
          <p className={dashboardStyles.empty}>You’re not in any groups yet. Create one, or find one in Discover.</p>
        ) : (
          <ul className={styles.groupList}>
            {groups.map((group) => <GroupCard key={group.id} group={group} viewerId={viewerId}/>)}
          </ul>
        )}
        {error && <p className={dashboardStyles.error}>{error}</p>}
        {cursor && (
          <div className={styles.loadMore}>
            <button type='button' className={dashboardStyles.buttonSecondary} disabled={loading} onClick={loadMore}>
              {loading ? 'Loading…' : 'Load more'}
            </button>
          </div>
        )}
      </section>
    </>
  );
}
