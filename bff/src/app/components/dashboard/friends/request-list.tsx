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

interface RequestListProps {
  direction: 'incoming' | 'outgoing';
  initial: CursorPage<FriendshipResponse> | null;
}

// Pending friend requests in one direction: incoming ones can be accepted, declined, or blocked;
// outgoing ones can be cancelled.
export default function RequestList({direction, initial}: RequestListProps) {
  const incoming = direction === 'incoming';
  const requests = useCursorList(`/api/users/me/friends/requests/${direction}`, initial,
    'Couldn’t load friend requests.');
  const {run, busy, errorFor} = usePostAction();

  async function act(request: FriendshipResponse, url: string, body?: object, method?: string) {
    if (await run(request.username, url, body, method)) {
      requests.setItems((current) => current.filter((r) => r.userId !== request.userId));
    }
  }

  const titleId = `${direction}-requests-title`;
  return (
    <section className={dashboardStyles.card} aria-labelledby={titleId}>
      <div className={dashboardStyles.cardHeader}>
        <h2 id={titleId} className={dashboardStyles.cardTitle}>{incoming ? 'Incoming' : 'Sent by you'}</h2>
      </div>
      {requests.loaded && requests.items.length === 0 ? (
        <p className={dashboardStyles.empty}>
          {incoming ? 'No one has sent you a friend request.' : 'You have no pending friend requests.'}
        </p>
      ) : (
        <ul className={styles.list}>
          {requests.items.map((request) => {
            const base = `/api/users/me/friends/requests/${encodeURIComponent(request.username)}`;
            return (
              <UserRow key={request.userId} user={request} error={errorFor(request.username)}>
                <div className={dashboardStyles.actions}>
                  {incoming ? (
                    <>
                      <button type='button' className={dashboardStyles.button} disabled={busy}
                              onClick={() => act(request, `${base}/accept`)}>
                        Accept
                      </button>
                      <button type='button' className={dashboardStyles.buttonSecondary} disabled={busy}
                              onClick={() => act(request, `${base}/decline`)}>
                        Decline
                      </button>
                      <ConfirmButton label='Block' confirmLabel={`Block @${request.username}`} disabled={busy}
                                     onConfirm={() => act(request, '/api/users/me/friends/blocked',
                                       {username: request.username})}/>
                    </>
                  ) : (
                    <button type='button' className={dashboardStyles.buttonSecondary} disabled={busy}
                            onClick={() => act(request, base, undefined, 'DELETE')}>
                      Cancel request
                    </button>
                  )}
                </div>
              </UserRow>
            );
          })}
        </ul>
      )}
      <LoadMore list={requests}/>
    </section>
  );
}
