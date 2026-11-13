'use client';

import {useState, useSyncExternalStore} from 'react';
import Link from 'next/link';
import authStyles from '@/app/components/auth/auth.module.css';
import styles from './invite-link.module.css';
import {formatMeetingTime} from '@/app/components/dashboard/home/format';
import {readApiError} from '@/lib/api-error';
import type {InviteResponse} from '@/lib/types/invite';

type Action = 'accept' | 'decline' | 'leave';

interface InviteLinkActionsProps {
  kind: 'group' | 'meeting';
  token: string;
  invite: InviteResponse;
  // Set when someone is signed in who isn't the recipient (the recipient is redirected instead).
  viewerUsername: string | null;
  // Set when the recipient has an account and nobody is signed in.
  loginHref: string | null;
}

const FAILED_MESSAGE = 'Something went wrong. Please try again.';

const subscribeNoop = () => () => {};

// The viewer's browser time zone, or null during server render. Invite recipients often have no
// profile time zone, and the server renders in its own (UTC), so the time is filled in on the client.
function useBrowserTimeZone(): string | null {
  return useSyncExternalStore(
    subscribeNoop,
    () => Intl.DateTimeFormat().resolvedOptions().timeZone,
    () => null
  );
}

export default function InviteLinkActions({
  kind,
  token,
  invite: initialInvite,
  viewerUsername,
  loginHref,
}: InviteLinkActionsProps) {
  const [invite, setInvite] = useState(initialInvite);
  const [pending, setPending] = useState<Action | null>(null);
  const [error, setError] = useState<string | null>(null);
  // Accepting a meeting invite rotates its token (the confirmation email carries the new one), so
  // this page's token can't be used to leave afterward.
  const [tokenSpent, setTokenSpent] = useState(false);
  const timeZone = useBrowserTimeZone();

  async function respond(action: Action) {
    if (pending) return;
    setPending(action);
    setError(null);
    try {
      const response = await fetch(
        `/api/invite-links/${kind}/${encodeURIComponent(token)}/${action}`,
        {method: 'POST'}
      );
      if (!response.ok) {
        const err = await readApiError(response);
        setError(err?.message ?? FAILED_MESSAGE);
        return;
      }
      setInvite((await response.json()) as InviteResponse);
      if (kind === 'meeting' && action === 'accept') setTokenSpent(true);
    } catch {
      setError(FAILED_MESSAGE);
    } finally {
      setPending(null);
    }
  }

  const status = statusText(invite, kind, tokenSpent);
  const sender = invite.senderUsername ?? 'Someone';
  const target = kind === 'group'
    ? <>the group <strong>{invite.groupName ?? 'a group'}</strong></>
    : <>the meeting <strong>{invite.meetingTitle ?? 'a meeting'}</strong></>;
  const when = invite.meetingStartTime && timeZone
    ? formatMeetingTime(invite.meetingStartTime, invite.meetingEndTime, timeZone)
    : null;
  const location = invite.meetingLocation?.trim();

  return (
    <>
      <p className={authStyles.subtitle}>
        {sender} invited you to {target}.
      </p>

      {kind === 'meeting' && (invite.meetingStartTime || location) && (
        <dl className={styles.details}>
          {invite.meetingStartTime && (
            <div className={styles.detailRow}>
              <dt>When</dt>
              <dd>
                {when ? (
                  <>
                    {when.day} · {when.time}
                    <span className={styles.timeZone}> ({timeZone})</span>
                  </>
                ) : '…'}
              </dd>
            </div>
          )}
          {location && (
            <div className={styles.detailRow}>
              <dt>Where</dt>
              <dd>{location}</dd>
            </div>
          )}
        </dl>
      )}

      {viewerUsername && (
        <p className={styles.notice}>
          This invite was sent to a different account than the one you&apos;re signed in as
          ({viewerUsername}).
        </p>
      )}

      {status && <p className={styles.status}>{status}</p>}

      {error && (
        <p className={authStyles.error} role='alert'>
          {error}
        </p>
      )}

      {invite.status === 'PENDING' && (
        <div className={styles.actions}>
          <button
            className={authStyles.submit}
            type='button'
            disabled={pending !== null}
            onClick={() => respond('accept')}
          >
            {pending === 'accept' ? 'Accepting...' : 'Accept'}
          </button>
          <button
            className={styles.secondary}
            type='button'
            disabled={pending !== null}
            onClick={() => respond('decline')}
          >
            {pending === 'decline' ? 'Declining...' : 'Decline'}
          </button>
        </div>
      )}

      {invite.status === 'ACCEPTED' && kind === 'meeting' && !tokenSpent && (
        <div className={styles.actions}>
          <button
            className={styles.secondary}
            type='button'
            disabled={pending !== null}
            onClick={() => respond('leave')}
          >
            {pending === 'leave' ? 'Leaving...' : 'Leave meeting'}
          </button>
        </div>
      )}

      {loginHref && (
        <p className={authStyles.altLink}>
          Have an account? <Link href={loginHref}>Log in</Link> to manage this from your dashboard.
        </p>
      )}
    </>
  );
}

function statusText(invite: InviteResponse, kind: 'group' | 'meeting', tokenSpent: boolean): string | null {
  switch (invite.status) {
    case 'PENDING':
      return null;
    case 'ACCEPTED':
      if (kind === 'group') return 'You\'ve joined this group.';
      return tokenSpent
        ? 'You\'re attending. We\'ve emailed you a confirmation with a link to leave if your plans change.'
        : 'You\'re attending this meeting.';
    case 'DECLINED':
      return 'You declined this invite.';
    case 'LEFT':
      return 'You left this meeting.';
  }
}
