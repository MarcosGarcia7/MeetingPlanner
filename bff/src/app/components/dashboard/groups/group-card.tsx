'use client';

import Link from 'next/link';
import {LockClosedIcon} from '@heroicons/react/24/solid';
import type {GroupSummaryResponse} from '@/lib/types/group';
import {usePostAction} from '@/app/components/dashboard/use-post-action';
import dashboardStyles from '@/app/components/dashboard/dashboard.module.css';
import styles from './groups.module.css';

interface GroupCardProps {
  group: GroupSummaryResponse;
  // Called after a successful action, for lists the server page refresh doesn't cover (search results).
  onChanged?: () => void;
}

function plural(count: number, word: string) {
  return `${count} ${word}${count === 1 ? '' : 's'}`;
}

export default function GroupCard({group, onChanged}: GroupCardProps) {
  const {run, busy, errorFor} = usePostAction();
  const key = `group-${group.id}`;
  const error = errorFor(key);

  async function act(url: string) {
    if (await run(key, url)) {
      onChanged?.();
    }
  }

  function renderActions() {
    switch (group.viewerStatus) {
      case 'MEMBER':
        return (
          <Link href={`/dashboard/groups?group=${group.id}`} className={dashboardStyles.buttonSecondary}>
            Open
          </Link>
        );
      case 'INVITED':
        return (
          <>
            <button type='button' className={dashboardStyles.button} disabled={busy}
                    onClick={() => act(`/api/invites/group/${group.pendingInviteId}/accept`)}>
              Accept invite
            </button>
            <button type='button' className={dashboardStyles.buttonSecondary} disabled={busy}
                    onClick={() => act(`/api/invites/group/${group.pendingInviteId}/decline`)}>
              Decline
            </button>
          </>
        );
      case 'REQUESTED':
        return (
          <>
            <span className={dashboardStyles.meta}>Requested</span>
            <button type='button' className={dashboardStyles.buttonSecondary} disabled={busy}
                    onClick={() => act(`/api/requests/group/${group.pendingRequestId}/cancel`)}>
              Cancel
            </button>
          </>
        );
      case 'BANNED':
        return <span className={dashboardStyles.meta}>Unavailable</span>;
      default:
        return group.isPrivate ? (
          <button type='button' className={dashboardStyles.button} disabled={busy}
                  onClick={() => act(`/api/groups/${group.id}/join-requests`)}>
            Request to join
          </button>
        ) : (
          <button type='button' className={dashboardStyles.button} disabled={busy}
                  onClick={() => act(`/api/groups/${group.id}/join`)}>
            Join
          </button>
        );
    }
  }

  return (
    <li className={styles.groupCard}>
      <div className={styles.groupBody}>
        <span className={styles.groupName}>
          {group.groupName}
          {group.isPrivate && (
            <span className={styles.privateBadge}>
              <LockClosedIcon className={styles.badgeIcon} aria-hidden/>
              Private
            </span>
          )}
        </span>
        <span className={dashboardStyles.meta}>
          {plural(group.memberCount, 'member')}
          {group.friendMemberCount > 0 && ` · ${plural(group.friendMemberCount, 'friend')} in this group`}
        </span>
        {error && <span className={dashboardStyles.error}>{error}</span>}
      </div>
      <div className={dashboardStyles.actions}>{renderActions()}</div>
    </li>
  );
}
