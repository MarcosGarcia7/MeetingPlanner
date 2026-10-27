'use client';

import {EllipsisHorizontalIcon} from '@heroicons/react/24/solid';
import type {CursorPage} from '@/lib/types/api';
import type {FriendshipResponse} from '@/lib/types/friend';
import ConfirmButton from '@/app/components/dashboard/confirm-button';
import {useCursorList} from '@/app/components/dashboard/use-cursor-list';
import {usePostAction} from '@/app/components/dashboard/use-post-action';
import LoadMore from './load-more';
import UserRow from './user-row';
import dashboardStyles from '@/app/components/dashboard/dashboard.module.css';
import styles from './friends.module.css';

// Accepted friends, each with a menu to remove or block them.
export default function FriendList({initial}: { initial: CursorPage<FriendshipResponse> | null }) {
  const friends = useCursorList('/api/users/me/friends', initial, 'Couldn’t load your friends.');
  const {run, busy, errorFor} = usePostAction();

  async function act(friend: FriendshipResponse, url: string, body?: object, method?: string) {
    if (await run(friend.username, url, body, method)) {
      friends.setItems((current) => current.filter((f) => f.userId !== friend.userId));
    }
  }

  return (
    <section className={dashboardStyles.card} aria-labelledby='friends-title'>
      <div className={dashboardStyles.cardHeader}>
        <h2 id='friends-title' className={dashboardStyles.cardTitle}>Friends</h2>
      </div>
      {friends.loaded && friends.items.length === 0 ? (
        <p className={dashboardStyles.empty}>No friends yet. Search above to add some.</p>
      ) : (
        <ul className={styles.list}>
          {friends.items.map((friend) => (
            <UserRow key={friend.userId} user={friend} error={errorFor(friend.username)}>
              <details className={styles.menu}>
                <summary className={styles.menuButton} aria-label={`Actions for ${friend.username}`}>
                  <EllipsisHorizontalIcon className={styles.menuIcon} aria-hidden/>
                </summary>
                <div className={styles.menuPanel}>
                  <ConfirmButton label='Remove friend' confirmLabel='Remove' disabled={busy}
                                 onConfirm={() => act(friend,
                                   `/api/users/me/friends/${encodeURIComponent(friend.username)}`,
                                   undefined, 'DELETE')}/>
                  <ConfirmButton label='Block' confirmLabel={`Block @${friend.username}`} disabled={busy}
                                 onConfirm={() => act(friend, '/api/users/me/friends/blocked',
                                   {username: friend.username})}/>
                </div>
              </details>
            </UserRow>
          ))}
        </ul>
      )}
      <LoadMore list={friends}/>
    </section>
  );
}
