import Link from 'next/link';
import {headers} from 'next/headers';
import {notFound, redirect} from 'next/navigation';
import {LockClosedIcon} from '@heroicons/react/24/solid';
import {verifySession} from '@/lib/dal';
import {ApiError, AuthError, authFetch, getClientIp} from '@/lib/api-client';
import type {GroupResponse} from '@/lib/types/group';
import dashboardStyles from '@/app/components/dashboard/dashboard.module.css';
import styles from '@/app/components/dashboard/groups/groups.module.css';

async function loadGroup(groupId: string, clientIp: string | undefined): Promise<GroupResponse> {
  try {
    return await authFetch<GroupResponse>(`/api/groups/${encodeURIComponent(groupId)}`, {}, clientIp);
  } catch (error) {
    if (error instanceof AuthError) {
      redirect('/login');
    }
    if (error instanceof ApiError && (error.status === 400 || error.status === 404)) {
      notFound();
    }
    throw error;
  }
}

// A single group. For now just its header and members; posts, meetings, and Manage come next.
export default async function GroupPage({params}: PageProps<'/dashboard/groups/[groupId]'>) {
  const {userId} = await verifySession();
  const {groupId} = await params;
  const group = await loadGroup(groupId, getClientIp(await headers()));
  const members = group.members ?? [];

  return (
    <main className={dashboardStyles.page}>
      <Link href='/dashboard/groups/mine' className={dashboardStyles.cardLink}>← My groups</Link>
      <section className={dashboardStyles.card}>
        <div className={dashboardStyles.cardHeader}>
          <h1 className={styles.groupName}>
            {group.groupName}
            {group.ownerId === userId && <span className={styles.ownerBadge}>Owner</span>}
            {group.isPrivate && (
              <span className={styles.privateBadge}>
                <LockClosedIcon className={styles.badgeIcon} aria-hidden/>
                Private
              </span>
            )}
          </h1>
        </div>
        <p className={dashboardStyles.meta}>
          {group.memberCount} {group.memberCount === 1 ? 'member' : 'members'}
        </p>
      </section>
      {members.length > 0 && (
        <section className={dashboardStyles.card}>
          <div className={dashboardStyles.cardHeader}>
            <h2 className={dashboardStyles.cardTitle}>Members</h2>
          </div>
          <ul className={styles.groupList}>
            {members.map((member) => (
              <li key={member.userId} className={styles.groupCard}>
                <span className={styles.groupName}>
                  {member.username}
                  {member.userId === group.ownerId ? (
                    <span className={styles.ownerBadge}>Owner</span>
                  ) : member.isAdmin && (
                    <span className={styles.ownerBadge}>Admin</span>
                  )}
                </span>
              </li>
            ))}
          </ul>
        </section>
      )}
    </main>
  );
}
