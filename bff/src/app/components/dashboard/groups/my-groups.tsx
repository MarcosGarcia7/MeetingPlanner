'use client';

import {useState} from 'react';
import type {CursorPage} from '@/lib/types/api';
import type {GroupSummaryResponse} from '@/lib/types/group';
import {useCursorList} from '@/app/components/dashboard/use-cursor-list';
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
  const [creating, setCreating] = useState(initiallyCreating);
  const groups = useCursorList('/api/groups', initial, 'Couldn’t load your groups.');

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
        {groups.loaded && groups.items.length === 0 ? (
          <p className={dashboardStyles.empty}>You’re not in any groups yet. Create one, or find one in Discover.</p>
        ) : (
          <ul className={styles.groupList}>
            {groups.items.map((group) => <GroupCard key={group.id} group={group} viewerId={viewerId}/>)}
          </ul>
        )}
        {groups.error && <p className={dashboardStyles.error}>{groups.error}</p>}
        {groups.hasMore && (
          <div className={styles.loadMore}>
            <button type='button' className={dashboardStyles.buttonSecondary} disabled={groups.loading}
                    onClick={groups.loadMore}>
              {groups.loading ? 'Loading…' : 'Load more'}
            </button>
          </div>
        )}
      </section>
    </>
  );
}
