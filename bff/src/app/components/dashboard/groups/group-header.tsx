'use client';

import {useState} from 'react';
import {useRouter} from 'next/navigation';
import {LockClosedIcon} from '@heroicons/react/24/solid';
import type {GroupPrivacyRequest, GroupResponse} from '@/lib/types/group';
import {sendJson} from '@/lib/send-json';
import ConfirmButton from '@/app/components/dashboard/confirm-button';
import type {GroupRole} from './load-group';
import dashboardStyles from '@/app/components/dashboard/dashboard.module.css';
import styles from './groups.module.css';

const ROLE_LABELS: Record<GroupRole, string | null> = {OWNER: 'Owner', ADMIN: 'Admin', MEMBER: null};

// Name, badges, and member count, plus the viewer's own settings: sharing their schedule with the group,
// and leaving it. The owner can't leave; they transfer ownership (Members) or delete the group (Manage).
export default function GroupHeader({group, role}: { group: GroupResponse; role: GroupRole | null }) {
  const router = useRouter();
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [shareSchedule, setShareSchedule] = useState(group.viewerShareSchedule ?? false);
  const roleLabel = role && ROLE_LABELS[role];

  async function toggleShareSchedule() {
    const body: GroupPrivacyRequest = {shareSchedule: !shareSchedule};
    setBusy(true);
    setError(null);
    const result = await sendJson(`/api/groups/${group.id}/privacy`, 'PATCH', body);
    setBusy(false);
    if (!result.ok) {
      if (result.unauthenticated) {
        router.push('/login');
        return;
      }
      setError(result.message);
      return;
    }
    setShareSchedule(body.shareSchedule);
  }

  async function leave() {
    setBusy(true);
    setError(null);
    const result = await sendJson(`/api/groups/${group.id}/leave`, 'POST');
    if (!result.ok) {
      setBusy(false);
      if (result.unauthenticated) {
        router.push('/login');
        return;
      }
      setError(result.message);
      return;
    }
    router.push('/dashboard/groups/mine');
    router.refresh();
  }

  return (
    <section className={dashboardStyles.card}>
      <div className={styles.groupHeader}>
        <div className={styles.groupBody}>
          <h1 className={styles.groupTitle}>
            {group.groupName}
            {roleLabel && <span className={styles.ownerBadge}>{roleLabel}</span>}
            {group.isPrivate && (
              <span className={styles.privateBadge}>
                <LockClosedIcon className={styles.badgeIcon} aria-hidden/>
                Private
              </span>
            )}
          </h1>
          <span className={dashboardStyles.meta}>
            {group.memberCount} {group.memberCount === 1 ? 'member' : 'members'}
          </span>
          {role && (
            <label className={styles.checkboxField}>
              <input type='checkbox' checked={shareSchedule} disabled={busy} onChange={toggleShareSchedule}/>
              Share my schedule with this group
            </label>
          )}
          {error && <span className={dashboardStyles.error} role='alert'>{error}</span>}
        </div>
        {(role === 'ADMIN' || role === 'MEMBER') && (
          <div className={dashboardStyles.actions}>
            <ConfirmButton label='Leave group' confirmLabel='Leave' disabled={busy} onConfirm={leave}/>
          </div>
        )}
      </div>
    </section>
  );
}
