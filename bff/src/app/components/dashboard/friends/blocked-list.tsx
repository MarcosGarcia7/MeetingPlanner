'use client';

import type {CursorPage} from '@/lib/types/api';
import type {FriendshipResponse} from '@/lib/types/friend';
import ConfirmButton from '@/app/components/dashboard/confirm-button';
import {useCursorList} from '@/app/components/dashboard/use-cursor-list';
import {usePostAction} from '@/app/components/dashboard/use-post-action';
import LoadMore from './load-more';
import UserRow from './user-row';
import dashboardStyles from '@/app/components/dashboard/dashboard.module.css';
import styles from './friends.module.css';

// People the viewer blocked, with unblock. Unblocking clears the pair, so either side can send a request again.
export default function BlockedList({initial}: { initial: CursorPage<FriendshipResponse> | null }) {
  const blocked = useCursorList('/api/users/me/friends/blocked', initial, 'Couldn’t load blocked users.');
  const {run, busy, errorFor} = usePostAction();

  async function unblock(user: FriendshipResponse) {
    const url = `/api/users/me/friends/blocked/${encodeURIComponent(user.username)}`;
    if (await run(user.username, url, undefined, 'DELETE')) {
      blocked.setItems((current) => current.filter((b) => b.userId !== user.userId));
    }
  }

  return (
    <section className={dashboardStyles.card} aria-labelledby='blocked-title'>
      <div className={dashboardStyles.cardHeader}>
        <h2 id='blocked-title' className={dashboardStyles.cardTitle}>Blocked</h2>
      </div>
      <p className={dashboardStyles.meta}>
        People you block can’t send you friend requests or invites, or add you to groups and meetings.
      </p>
      {blocked.loaded && blocked.items.length === 0 ? (
        <p className={dashboardStyles.empty}>You haven’t blocked anyone.</p>
      ) : (
        <ul className={styles.list}>
          {blocked.items.map((user) => (
            <UserRow key={user.userId} user={user} error={errorFor(user.username)}>
              <div className={dashboardStyles.actions}>
                <ConfirmButton label='Unblock' confirmLabel={`Unblock @${user.username}`} disabled={busy}
                               onConfirm={() => unblock(user)}/>
              </div>
            </UserRow>
          ))}
        </ul>
      )}
      <LoadMore list={blocked}/>
    </section>
  );
}
