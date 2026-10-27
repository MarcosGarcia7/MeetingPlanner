'use client';

import {useId, useState} from 'react';
import {XMarkIcon} from '@heroicons/react/24/solid';
import {z} from 'zod';
import type {MeetingInviteRequest} from '@/lib/types/meeting';
import authStyles from '@/app/components/auth/auth.module.css';
import dashboardStyles from '@/app/components/dashboard/dashboard.module.css';
import styles from './groups.module.css';

// Mirrors MeetingInviteBatchRequest / MeetingInviteRequest limits.
export const MAX_INVITEES = 50;
const emailSchema = z.email().max(254);
const usernameSchema = z.string().min(1).max(20).regex(/^[^\s@]+$/);

// Anything with an @ is an email, otherwise a username (a leading @ is dropped).
export function toInviteRequest(value: string): MeetingInviteRequest {
  return value.includes('@') && !value.startsWith('@') ? {email: value} : {username: value.replace(/^@/, '')};
}

function validate(value: string, allowEmail: boolean): string | null {
  const request = toInviteRequest(value);
  if (request.email && !allowEmail) {
    return 'Enter a username';
  }
  if (request.email) {
    return emailSchema.safeParse(request.email).success ? null : 'That doesn’t look like an email address';
  }
  return usernameSchema.safeParse(request.username).success ? null : 'Usernames are 1–20 characters, no spaces';
}

interface InviteeInputProps {
  value: string[];
  onChange: (value: string[]) => void;
  // Offered as suggestions while typing (the group's other members).
  suggestions: string[];
  disabled?: boolean;
  // Group invites take usernames only; meeting invites also take emails.
  allowEmail?: boolean;
  label?: string;
}

// Collects usernames and emails as removable chips. Enter, comma, Add, or leaving the field commits what's
// typed, so an entry isn't lost when the form is submitted straight away.
export default function InviteeInput({value, onChange, suggestions, disabled, allowEmail = true,
                                       label = 'Invite people (optional)'}: InviteeInputProps) {
  const listId = useId();
  const [draft, setDraft] = useState('');
  const [error, setError] = useState<string | null>(null);
  const taken = new Set(value.map((v) => v.toLowerCase()));

  function add() {
    const entry = draft.trim().replace(/,$/, '');
    if (!entry) {
      return;
    }
    const problem = validate(entry, allowEmail)
      ?? (taken.has(entry.replace(/^@/, '').toLowerCase()) ? 'Already added' : null)
      ?? (value.length >= MAX_INVITEES ? `You can invite up to ${MAX_INVITEES} people at once` : null);
    if (problem) {
      setError(problem);
      return;
    }
    onChange([...value, entry.replace(/^@/, '')]);
    setDraft('');
    setError(null);
  }

  return (
    <div className={authStyles.field}>
      <span className={authStyles.label}>{label}</span>
      {value.length > 0 && (
        <ul className={styles.chips} aria-label='People to invite'>
          {value.map((entry) => (
            <li key={entry} className={styles.chip}>
              {entry.includes('@') ? entry : `@${entry}`}
              <button type='button' className={styles.chipRemove} aria-label={`Remove ${entry}`} disabled={disabled}
                      onClick={() => onChange(value.filter((v) => v !== entry))}>
                <XMarkIcon className={styles.badgeIcon} aria-hidden/>
              </button>
            </li>
          ))}
        </ul>
      )}
      <div className={styles.commentForm}>
        <input type='text' list={listId} maxLength={254} placeholder={allowEmail ? 'Username or email' : 'Username'} value={draft}
               aria-label={allowEmail ? 'Username or email to invite' : 'Username to invite'} disabled={disabled} className={authStyles.input}
               onChange={(event) => {
                 setDraft(event.target.value);
                 setError(null);
               }}
               onBlur={add}
               onKeyDown={(event) => {
                 // Enter would submit the meeting form; here it adds the invitee instead.
                 if (event.key === 'Enter' || event.key === ',') {
                   event.preventDefault();
                   add();
                 }
               }}/>
        <button type='button' className={dashboardStyles.buttonSecondary} disabled={disabled || !draft.trim()}
                onClick={add}>
          Add
        </button>
      </div>
      <datalist id={listId}>
        {suggestions.filter((s) => !taken.has(s.toLowerCase())).map((s) => <option key={s} value={s}/>)}
      </datalist>
      {error && <span className={authStyles.error} role='alert'>{error}</span>}
    </div>
  );
}
