'use client';

import Image from 'next/image';
import {EllipsisHorizontalIcon} from '@heroicons/react/24/solid';
import {avatarDataUri} from '@/lib/avatar';
import type {GroupMemberResponse} from '@/lib/types/group';
import {AVATAR_COLORS} from '@/app/components/dashboard/profile/avatar-colors';
import ConfirmButton from '@/app/components/dashboard/confirm-button';
import {usePostAction} from '@/app/components/dashboard/use-post-action';
import type {GroupRole} from './load-group';
import dashboardStyles from '@/app/components/dashboard/dashboard.module.css';
import styles from './groups.module.css';

interface MemberListProps {
  groupId: number;
  ownerId: number | null;
  members: GroupMemberResponse[];
  viewerId: number;
  viewerRole: GroupRole;
}

type Action = 'toggleAdmin' | 'remove' | 'ban' | 'transfer';

// What the viewer may do to `member`, mirroring the server: the owner can do anything to anyone else;
// admins can remove or ban plain members; nobody acts on themselves or on the owner.
function actionsFor(viewerRole: GroupRole, viewerId: number, ownerId: number | null,
                    member: GroupMemberResponse): Action[] {
  if (member.userId === viewerId || member.userId === ownerId) {
    return [];
  }
  if (viewerRole === 'OWNER') {
    return ['toggleAdmin', 'remove', 'ban', 'transfer'];
  }
  if (viewerRole === 'ADMIN' && !member.isAdmin) {
    return ['remove', 'ban'];
  }
  return [];
}

// Members tab: owner first, then admins, with each row's actions in a menu. Every action re-renders
// the page so the list, header, and tabs pick up the change (e.g. a transfer).
export default function MemberList({groupId, ownerId, members, viewerId, viewerRole}: MemberListProps) {
  const {run, busy, errorFor} = usePostAction();

  return (
    <section className={dashboardStyles.card}>
      <div className={dashboardStyles.cardHeader}>
        <h2 className={dashboardStyles.cardTitle}>Members</h2>
      </div>
      <ul className={styles.groupList}>
        {members.map((member) => {
          const key = `member-${member.userId}`;
          const error = errorFor(key);
          const actions = actionsFor(viewerRole, viewerId, ownerId, member);
          const memberUrl = `/api/groups/${groupId}/members/${member.userId}`;
          return (
            <li key={key} className={styles.groupCard}>
              <Image src={avatarDataUri({
                username: member.username ?? '',
                firstName: member.firstName,
                lastName: member.lastName,
                avatarColor: member.avatarColor ?? AVATAR_COLORS[0],
              })} alt='' width={40} height={40} className={dashboardStyles.avatar} unoptimized/>
              <div className={styles.groupBody}>
                <span className={styles.groupName}>
                  @{member.username}
                  {member.userId === viewerId && <span className={dashboardStyles.meta}>(you)</span>}
                  {member.userId === ownerId ? (
                    <span className={styles.ownerBadge}>Owner</span>
                  ) : member.isAdmin && (
                    <span className={styles.ownerBadge}>Admin</span>
                  )}
                </span>
                {error && <span className={dashboardStyles.error} role='alert'>{error}</span>}
              </div>
              {actions.length > 0 && (
                <details className={styles.menu}>
                  <summary className={styles.menuButton} aria-label={`Actions for ${member.username}`}>
                    <EllipsisHorizontalIcon className={styles.menuIcon} aria-hidden/>
                  </summary>
                  <div className={styles.menuPanel}>
                    {actions.includes('toggleAdmin') && (
                      <button type='button' className={dashboardStyles.buttonSecondary} disabled={busy}
                              onClick={() => run(key, memberUrl, undefined, 'PATCH')}>
                        {member.isAdmin ? 'Remove admin' : 'Make admin'}
                      </button>
                    )}
                    {actions.includes('remove') && (
                      <ConfirmButton label='Remove from group' confirmLabel='Remove' disabled={busy}
                                     onConfirm={() => run(key, memberUrl, undefined, 'DELETE')}/>
                    )}
                    {actions.includes('ban') && (
                      <ConfirmButton label='Ban' confirmLabel='Ban and remove' disabled={busy}
                                     onConfirm={() => run(key, `/api/groups/${groupId}/bans/${member.userId}`)}/>
                    )}
                    {actions.includes('transfer') && (
                      <ConfirmButton label='Make owner' confirmLabel={`Transfer to @${member.username}`}
                                     disabled={busy}
                                     onConfirm={() => run(key, `/api/groups/${groupId}/owner/${member.userId}`,
                                       undefined, 'PUT')}/>
                    )}
                  </div>
                </details>
              )}
            </li>
          );
        })}
      </ul>
    </section>
  );
}
