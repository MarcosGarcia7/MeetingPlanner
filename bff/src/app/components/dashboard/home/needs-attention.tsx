'use client';

import Link from 'next/link';
import type {CursorPage} from '@/lib/types/api';
import type {FriendshipResponse} from '@/lib/types/friend';
import type {InviteResponse} from '@/lib/types/invite';
import {usePostAction} from '@/app/components/dashboard/use-post-action';
import dashboardStyles from '@/app/components/dashboard/dashboard.module.css';
import styles from './home.module.css';

interface NeedsAttentionProps {
  invites: CursorPage<InviteResponse> | null;
  friendRequests: CursorPage<FriendshipResponse> | null;
}

interface Item {
  key: string;
  title: React.ReactNode;
  details: string[];
  acceptUrl: string;
  declineUrl: string;
}

function inviteItem(invite: InviteResponse): Item {
  const kind = invite.kind === 'GROUP' ? 'group' : 'meeting';
  const target = invite.kind === 'GROUP' ? invite.groupName : invite.meetingTitle;
  const base = `/api/invites/${kind}/${invite.id}`;
  return {
    key: `invite-${kind}-${invite.id}`,
    title: (
      <>
        @{invite.senderUsername} invited you to {invite.kind === 'GROUP' ? 'join ' : ''}
        <strong>{target ?? `a ${kind}`}</strong>
      </>
    ),
    details: invite.warnings,
    acceptUrl: `${base}/accept`,
    declineUrl: `${base}/decline`,
  };
}

function friendRequestItem(request: FriendshipResponse): Item {
  const fullName = [request.firstName, request.lastName].filter(Boolean).join(' ');
  const base = `/api/users/me/friends/requests/${encodeURIComponent(request.username)}`;
  return {
    key: `friend-${request.userId}`,
    title: (
      <>
        <strong>{fullName || `@${request.username}`}</strong>
        {fullName && ` (@${request.username})`} wants to be friends
      </>
    ),
    details: [],
    acceptUrl: `${base}/accept`,
    declineUrl: `${base}/decline`,
  };
}

export default function NeedsAttention({invites, friendRequests}: NeedsAttentionProps) {
  const {run, busy, errorFor} = usePostAction();

  const items = [
    ...(friendRequests?.items.map(friendRequestItem) ?? []),
    ...(invites?.items.map(inviteItem) ?? []),
  ];
  const failed = invites === null || friendRequests === null;
  const hasMore = Boolean(invites?.nextCursor || friendRequests?.nextCursor);

  return (
    <section className={dashboardStyles.card}>
      <div className={dashboardStyles.cardHeader}>
        <h2 className={dashboardStyles.cardTitle}>Needs your attention</h2>
      </div>
      {failed && <p className={dashboardStyles.error}>Some invites or requests couldn’t be loaded.</p>}
      {items.length === 0 ? (
        !failed && <p className={dashboardStyles.empty}>No pending invites or friend requests.</p>
      ) : (
        <ul className={styles.list}>
          {items.map((item) => {
            const error = errorFor(item.key);
            return (
              <li key={item.key} className={styles.item}>
                <div className={styles.itemBody}>
                  <span className={styles.itemTitle}>{item.title}</span>
                  {item.details.map((detail) => (
                    <span key={detail} className={dashboardStyles.meta}>{detail}</span>
                  ))}
                  {error && <span className={dashboardStyles.error}>{error}</span>}
                </div>
                <div className={dashboardStyles.actions}>
                  <button type='button' className={dashboardStyles.button} disabled={busy}
                          onClick={() => run(item.key, item.acceptUrl)}>
                    Accept
                  </button>
                  <button type='button' className={dashboardStyles.buttonSecondary} disabled={busy}
                          onClick={() => run(item.key, item.declineUrl)}>
                    Decline
                  </button>
                </div>
              </li>
            );
          })}
        </ul>
      )}
      {hasMore && (
        <div className={dashboardStyles.cardFooter}>
          <Link href='/dashboard/inbox' className={dashboardStyles.cardLink}>See all</Link>
        </div>
      )}
    </section>
  );
}
