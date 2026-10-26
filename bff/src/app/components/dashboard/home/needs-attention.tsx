'use client';

import Link from 'next/link';
import type {CursorPage} from '@/lib/types/api';
import type {FriendshipResponse} from '@/lib/types/friend';
import type {InviteResponse} from '@/lib/types/invite';
import {usePostAction} from './use-post-action';
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
    <section className={styles.card}>
      <div className={styles.cardHeader}>
        <h2 className={styles.cardTitle}>Needs your attention</h2>
      </div>
      {failed && <p className={styles.error}>Some invites or requests couldn’t be loaded.</p>}
      {items.length === 0 ? (
        !failed && <p className={styles.empty}>No pending invites or friend requests.</p>
      ) : (
        <ul className={styles.list}>
          {items.map((item) => {
            const error = errorFor(item.key);
            return (
              <li key={item.key} className={styles.item}>
                <div className={styles.itemBody}>
                  <span className={styles.itemTitle}>{item.title}</span>
                  {item.details.map((detail) => (
                    <span key={detail} className={styles.meta}>{detail}</span>
                  ))}
                  {error && <span className={styles.error}>{error}</span>}
                </div>
                <div className={styles.actions}>
                  <button type='button' className={styles.button} disabled={busy}
                          onClick={() => run(item.key, item.acceptUrl)}>
                    Accept
                  </button>
                  <button type='button' className={styles.buttonSecondary} disabled={busy}
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
        <div className={styles.cardFooter}>
          <Link href='/dashboard/inbox' className={styles.cardLink}>See all</Link>
        </div>
      )}
    </section>
  );
}
