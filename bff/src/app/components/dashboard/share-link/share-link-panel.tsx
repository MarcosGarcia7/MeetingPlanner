'use client';

import {useState} from 'react';
import {useRouter} from 'next/navigation';
import {useForm} from 'react-hook-form';
import {zodResolver} from '@hookform/resolvers/zod';
import {z} from 'zod';
import authStyles from '@/app/components/auth/auth.module.css';
import dashboardStyles from '@/app/components/dashboard/dashboard.module.css';
import ConfirmButton from '@/app/components/dashboard/confirm-button';
import {formatMeetingTime} from '@/app/components/dashboard/home/format';
import {sendJson} from '@/lib/send-json';
import type {
  CreateGroupShareLinkRequest,
  CreateMeetingShareLinkRequest,
  ShareLinkJoinMode,
  ShareLinkResponse,
} from '@/lib/types/invite';
import styles from './share-link.module.css';

export type ShareLinkTarget = { kind: 'group'; groupId: number } | { kind: 'meeting'; meetingId: number };

interface ShareLinkPanelProps {
  target: ShareLinkTarget;
  initial: ShareLinkResponse | null;
  timeZone: string;
}

const linkSchema = z.object({
  expiry: z.enum(['1', '7', '30', 'never']),
  maxUses: z.string().trim().refine(
    (v) => v === '' || (/^\d+$/.test(v) && Number(v) >= 1 && Number(v) <= 1000),
    'Enter a number from 1 to 1000, or leave it empty'
  ),
  joinMode: z.enum(['DIRECT', 'REQUEST']),
});
type LinkFormData = z.infer<typeof linkSchema>;

function apiPath(target: ShareLinkTarget): string {
  return target.kind === 'group'
    ? `/api/groups/${target.groupId}/share-link`
    : `/api/meetings/${target.meetingId}/share-link`;
}

// A link managers can copy into a message. Anyone signed in who opens it joins (or, for a group set to
// require approval, sends a join request); meeting links also let people without an account get an
// invite emailed to them. One link at a time: making a new one turns the old one off.
export default function ShareLinkPanel({target, initial, timeZone}: ShareLinkPanelProps) {
  const router = useRouter();
  const [link, setLink] = useState<ShareLinkResponse | null>(initial);
  const [editing, setEditing] = useState(initial === null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [copied, setCopied] = useState(false);
  const isGroup = target.kind === 'group';

  const {register, handleSubmit, formState: {errors}} = useForm<LinkFormData>({
    resolver: zodResolver(linkSchema),
    defaultValues: {
      expiry: '7',
      maxUses: initial?.maxUses != null ? String(initial.maxUses) : '',
      joinMode: initial?.joinMode ?? 'DIRECT',
    },
  });

  function handleFailure(result: { message: string; unauthenticated: boolean }) {
    if (result.unauthenticated) {
      router.push('/login');
      return;
    }
    setError(result.message);
  }

  async function create(data: LinkFormData) {
    setBusy(true);
    setError(null);
    const common = {
      expiresInDays: data.expiry === 'never' ? null : Number(data.expiry),
      maxUses: data.maxUses === '' ? null : Number(data.maxUses),
    };
    const body: CreateGroupShareLinkRequest | CreateMeetingShareLinkRequest = isGroup
      ? {...common, joinMode: data.joinMode as ShareLinkJoinMode}
      : common;
    const result = await sendJson<ShareLinkResponse>(apiPath(target), 'POST', body);
    setBusy(false);
    if (!result.ok) {
      handleFailure(result);
      return;
    }
    setLink(result.data);
    setEditing(false);
    setCopied(false);
  }

  async function disable() {
    setBusy(true);
    setError(null);
    const result = await sendJson(apiPath(target), 'DELETE');
    setBusy(false);
    if (!result.ok) {
      handleFailure(result);
      return;
    }
    setLink(null);
    setEditing(true);
  }

  async function copy() {
    if (!link) return;
    try {
      await navigator.clipboard.writeText(link.url);
      setCopied(true);
    } catch {
      setError('Couldn’t copy automatically. Select the link and copy it instead.');
    }
  }

  return (
    <section className={dashboardStyles.card} aria-labelledby='share-link-title'>
      <div className={dashboardStyles.cardHeader}>
        <h2 id='share-link-title' className={dashboardStyles.cardTitle}>Share link</h2>
      </div>
      <div className={styles.body}>
        {link ? (
          <>
            <div className={styles.linkRow}>
              <input className={styles.linkInput} type='text' readOnly value={link.url}
                     aria-label='Share link' onFocus={(e) => e.currentTarget.select()}/>
              <button type='button' className={dashboardStyles.button} onClick={copy} disabled={!link.usable}>
                {copied ? 'Copied' : 'Copy'}
              </button>
            </div>
            <p className={dashboardStyles.meta}>{describe(link, isGroup, timeZone)}</p>
            {!link.usable && (
              <p className={dashboardStyles.error}>This link no longer works. Create a new one to share.</p>
            )}
          </>
        ) : (
          <p className={dashboardStyles.meta}>
            Create a link you can paste into a message. {isGroup
              ? 'Anyone signed in who opens it can join the group, or ask to join if you require approval.'
              : 'Anyone who opens it can join; people without an account can get an invite by email.'}
          </p>
        )}

        {editing && (
          <form className={authStyles.form} onSubmit={handleSubmit(create)} noValidate>
            <div className={styles.fields}>
              <label className={authStyles.field}>
                <span className={authStyles.label}>Expires after</span>
                <select className={authStyles.input} {...register('expiry')}>
                  <option value='1'>1 day</option>
                  <option value='7'>7 days</option>
                  <option value='30'>30 days</option>
                  <option value='never'>Never</option>
                </select>
              </label>
              <label className={authStyles.field}>
                <span className={authStyles.label}>Max uses (optional)</span>
                <input className={authStyles.input} type='number' min={1} max={1000} inputMode='numeric'
                       placeholder='Unlimited' {...register('maxUses')}/>
              </label>
            </div>
            {errors.maxUses && <p className={authStyles.error} role='alert'>{errors.maxUses.message}</p>}
            {isGroup && (
              <fieldset className={styles.fieldset}>
                <legend className={authStyles.label}>People who open the link</legend>
                <label className={styles.radioField}>
                  <input type='radio' value='DIRECT' {...register('joinMode')}/>
                  Join the group right away
                </label>
                <label className={styles.radioField}>
                  <input type='radio' value='REQUEST' {...register('joinMode')}/>
                  Send a join request for an admin to approve
                </label>
              </fieldset>
            )}
            <div className={styles.footer}>
              {link && <span className={styles.status}>Creating a new link turns off the current one.</span>}
              {link && (
                <button type='button' className={dashboardStyles.buttonSecondary} disabled={busy}
                        onClick={() => setEditing(false)}>
                  Cancel
                </button>
              )}
              <button type='submit' className={dashboardStyles.button} disabled={busy}>
                {busy ? 'Creating…' : link ? 'Create new link' : 'Create link'}
              </button>
            </div>
          </form>
        )}

        {error && <p className={dashboardStyles.error} role='alert'>{error}</p>}

        {link && !editing && (
          <div className={styles.footer}>
            <button type='button' className={dashboardStyles.buttonSecondary} disabled={busy}
                    onClick={() => setEditing(true)}>
              New link…
            </button>
            <ConfirmButton label='Turn off link' confirmLabel='Turn off' disabled={busy} onConfirm={disable}/>
          </div>
        )}
      </div>
    </section>
  );
}

function describe(link: ShareLinkResponse, isGroup: boolean, timeZone: string): string {
  const parts: string[] = [];
  if (isGroup) {
    parts.push(link.joinMode === 'REQUEST' ? 'Joining needs approval' : 'Joins directly');
  }
  if (link.expiresAt) {
    const {day, time} = formatMeetingTime(link.expiresAt, null, timeZone);
    parts.push(`${new Date(link.expiresAt) > new Date() ? 'Expires' : 'Expired'} ${day}, ${time}`);
  } else {
    parts.push('Never expires');
  }
  parts.push(link.maxUses != null
    ? `Used ${link.useCount} of ${link.maxUses} times`
    : `Used ${link.useCount} ${link.useCount === 1 ? 'time' : 'times'}`);
  return parts.join(' · ');
}
