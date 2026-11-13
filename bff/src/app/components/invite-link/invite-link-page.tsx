import Image from 'next/image';
import Link from 'next/link';
import {headers} from 'next/headers';
import {redirect} from 'next/navigation';
import authStyles from '@/app/components/auth/auth.module.css';
import InviteLinkActions from './invite-link-actions';
import {ApiError, apiFetch, getClientIp} from '@/lib/api-client';
import {getSessionUser} from '@/lib/dal';
import {loginHref} from '@/lib/next-path';
import type {ApiErrorBody} from '@/lib/types/api';
import type {InviteResponse} from '@/lib/types/invite';

export type InviteLinkKind = 'group' | 'meeting';

interface InviteLinkPageProps {
  kind: InviteLinkKind;
  token: string | string[] | undefined;
}

// Where a signed-in recipient manages this invite: the group/meeting once accepted, else the inbox.
function dashboardPath(invite: InviteResponse): string {
  if (invite.status === 'ACCEPTED') {
    if (invite.kind === 'GROUP' && invite.groupId != null) return `/dashboard/groups/${invite.groupId}`;
    if (invite.kind === 'MEETING' && invite.meetingId != null) return `/dashboard/schedule/${invite.meetingId}`;
  }
  return '/dashboard/inbox/invites';
}

function errorMessage(err: ApiError): string {
  const body = err.body as Partial<ApiErrorBody> | null;
  return body?.message ?? 'This invite link is invalid or has expired.';
}

// Landing page for emailed invite links (`/invites/{kind}?token=…`). Anyone holding the token can
// respond, so recipients without an account can accept meeting invites. A signed-in recipient is
// sent to the dashboard instead.
export default async function InviteLinkPage({kind, token}: InviteLinkPageProps) {
  const viewer = await getSessionUser();
  const rawToken = typeof token === 'string' && token ? token : null;

  let invite: InviteResponse | null = null;
  let loadError: string | null = null;
  if (!rawToken) {
    loadError = 'This invite link is missing its token.';
  } else {
    try {
      invite = await apiFetch<InviteResponse>(
        `/api/invite-links/${kind}/${encodeURIComponent(rawToken)}`,
        {},
        getClientIp(await headers())
      );
    } catch (err) {
      if (!(err instanceof ApiError)) throw err;
      loadError = errorMessage(err);
    }
  }

  if (invite && viewer && invite.recipientId === viewer.userId) {
    redirect(dashboardPath(invite));
  }

  const selfPath = rawToken ? `/invites/${kind}?token=${encodeURIComponent(rawToken)}` : null;

  return (
    <main className={authStyles.page}>
      <div className={authStyles.card}>
        <Image
          className={authStyles.logo}
          src='/logo-mp.svg'
          alt='Meeting Planner logo'
          width={64}
          height={64}
          priority
        />
        <h1 className={authStyles.title}>
          {kind === 'group' ? 'Group invite' : 'Meeting invite'}
        </h1>
        {invite && selfPath && rawToken ? (
          <InviteLinkActions
            kind={kind}
            token={rawToken}
            invite={invite}
            viewerUsername={viewer?.username ?? null}
            loginHref={invite.recipientId != null && !viewer ? loginHref(selfPath) : null}
          />
        ) : (
          <>
            <p className={authStyles.error} role='alert'>
              {loadError}
            </p>
            <Link className={authStyles.linkButton} href={viewer ? '/dashboard/inbox/invites' : '/login'}>
              {viewer ? 'Go to your invites' : 'Go to login'}
            </Link>
          </>
        )}
      </div>
    </main>
  );
}
