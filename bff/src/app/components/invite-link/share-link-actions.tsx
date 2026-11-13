'use client';

import {useState} from 'react';
import Link from 'next/link';
import {useRouter} from 'next/navigation';
import {useForm} from 'react-hook-form';
import {zodResolver} from '@hookform/resolvers/zod';
import {z} from 'zod';
import authStyles from '@/app/components/auth/auth.module.css';
import styles from './invite-link.module.css';
import {formatMeetingTime} from '@/app/components/dashboard/home/format';
import {useBrowserTimeZone} from './use-browser-time-zone';
import {shareLinkDashboardPath} from './share-link-paths';
import {readApiError} from '@/lib/api-error';
import type {ShareLinkGuestRequest, ShareLinkJoinResponse, ShareLinkPreviewResponse} from '@/lib/types/invite';

const FAILED_MESSAGE = 'Something went wrong. Please try again.';

const guestSchema = z.object({
  email: z.string().trim().min(1, 'Enter your email').email('Enter a valid email address'),
});
type GuestFormData = z.infer<typeof guestSchema>;

interface ShareLinkActionsProps {
  token: string;
  preview: ShareLinkPreviewResponse;
  signedIn: boolean;
  // Login that returns to this page.
  loginHref: string;
}

export default function ShareLinkActions({token, preview, signedIn, loginHref}: ShareLinkActionsProps) {
  const isMeeting = preview.kind === 'MEETING';
  const requestOnly = !isMeeting && preview.joinMode === 'REQUEST';
  const timeZone = useBrowserTimeZone();

  const sender = preview.createdByUsername ?? 'Someone';
  const target = isMeeting
    ? <>the meeting <strong>{preview.meetingTitle ?? 'a meeting'}</strong></>
    : <>the group <strong>{preview.groupName ?? 'a group'}</strong></>;
  const when = preview.startTime && timeZone
    ? formatMeetingTime(preview.startTime, preview.endTime, timeZone)
    : null;
  const location = preview.location?.trim();

  return (
    <>
      <p className={authStyles.subtitle}>
        {sender} invited you to {requestOnly ? 'ask to join' : 'join'} {target}.
      </p>

      {isMeeting && (preview.startTime || location) && (
        <dl className={styles.details}>
          {preview.startTime && (
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

      {signedIn ? (
        <JoinButton token={token} isMeeting={isMeeting} requestOnly={requestOnly}/>
      ) : (
        <>
          <Link className={authStyles.linkButton} href={loginHref}>
            Log in to {requestOnly ? 'request to join' : 'join'}
          </Link>
          <p className={authStyles.altLink}>
            New here? <Link href='/signup'>Create an account</Link>, then open this link again.
          </p>
          {isMeeting && <GuestForm token={token}/>}
        </>
      )}
    </>
  );
}

function JoinButton({token, isMeeting, requestOnly}: { token: string; isMeeting: boolean; requestOnly: boolean }) {
  const router = useRouter();
  const [pending, setPending] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [requested, setRequested] = useState(false);

  async function join() {
    if (pending) return;
    setPending(true);
    setError(null);
    try {
      const response = await fetch(`/api/share-links/${encodeURIComponent(token)}/join`, {method: 'POST'});
      if (!response.ok) {
        const err = await readApiError(response);
        if (err?.error === 'unauthenticated') {
          router.refresh();
          return;
        }
        setError(err?.message ?? FAILED_MESSAGE);
        return;
      }
      const result = (await response.json()) as ShareLinkJoinResponse;
      if (result.outcome === 'REQUESTED') {
        setRequested(true);
        return;
      }
      router.push(shareLinkDashboardPath(result.kind, result.groupId, result.meetingId));
    } catch {
      setError(FAILED_MESSAGE);
    } finally {
      setPending(false);
    }
  }

  if (requested) {
    return (
      <>
        <p className={styles.status} role='status'>
          Request sent. You&apos;ll get a notification when an admin approves it.
        </p>
        <Link className={authStyles.linkButton} href='/dashboard'>
          Go to your dashboard
        </Link>
      </>
    );
  }

  const label = requestOnly ? 'Request to join' : isMeeting ? 'Join meeting' : 'Join group';
  return (
    <>
      {error && (
        <p className={authStyles.error} role='alert'>
          {error}
        </p>
      )}
      <div className={styles.actions}>
        <button className={authStyles.submit} type='button' disabled={pending} onClick={join}>
          {pending ? 'Joining...' : label}
        </button>
      </div>
    </>
  );
}

// For guests without an account: the API emails a regular meeting invite to the address, which proves it's
// theirs. The API answers the same whatever happens, so the message here can't confirm anything either.
function GuestForm({token}: { token: string }) {
  const [error, setError] = useState<string | null>(null);
  const [sentTo, setSentTo] = useState<string | null>(null);

  const {
    register,
    handleSubmit,
    formState: {errors, isSubmitting},
  } = useForm<GuestFormData>({
    resolver: zodResolver(guestSchema),
    defaultValues: {email: ''},
  });

  const onSubmit = async (data: GuestFormData) => {
    setError(null);
    const body: ShareLinkGuestRequest = {email: data.email};
    try {
      const response = await fetch(`/api/share-links/${encodeURIComponent(token)}/guest`, {
        method: 'POST',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify(body),
      });
      if (!response.ok) {
        const err = await readApiError(response);
        setError(err?.error === 'rate_limited'
          ? 'Too many requests. Please wait a few minutes and try again.'
          : err?.message ?? FAILED_MESSAGE);
        return;
      }
      setSentTo(data.email);
    } catch {
      setError(FAILED_MESSAGE);
    }
  };

  return (
    <>
      <p className={styles.divider}>or continue as a guest</p>
      {sentTo ? (
        <p className={styles.status} role='status'>
          Check your inbox at <strong>{sentTo}</strong>. If you can join this meeting, we&apos;ve sent an
          invite there. Open it to accept.
        </p>
      ) : (
        <form className={authStyles.form} onSubmit={handleSubmit(onSubmit)} noValidate>
          <label className={authStyles.field}>
            <span className={authStyles.label}>Email</span>
            <input className={authStyles.input} type='email' autoComplete='email' {...register('email')}/>
          </label>
          {errors.email && (
            <p className={authStyles.error} role='alert'>
              {errors.email.message}
            </p>
          )}
          {error && (
            <p className={authStyles.error} role='alert'>
              {error}
            </p>
          )}
          <button className={styles.secondary} type='submit' disabled={isSubmitting}>
            {isSubmitting ? 'Sending...' : 'Email me an invite'}
          </button>
        </form>
      )}
    </>
  );
}
