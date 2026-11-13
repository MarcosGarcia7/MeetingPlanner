import Image from 'next/image';
import Link from 'next/link';
import {headers} from 'next/headers';
import {redirect} from 'next/navigation';
import authStyles from '@/app/components/auth/auth.module.css';
import ShareLinkActions from './share-link-actions';
import {shareLinkDashboardPath} from './share-link-paths';
import {ApiError, AuthError, apiFetch, authFetch, getClientIp} from '@/lib/api-client';
import {getSessionUser} from '@/lib/dal';
import {loginHref} from '@/lib/next-path';
import type {ApiErrorBody} from '@/lib/types/api';
import type {ShareLinkPreviewResponse} from '@/lib/types/invite';

async function loadPreview(token: string, signedIn: boolean): Promise<ShareLinkPreviewResponse> {
  const path = `/api/share-links/${encodeURIComponent(token)}`;
  const clientIp = getClientIp(await headers());
  if (signedIn) {
    try {
      // With a bearer the API also says whether the viewer is already a member.
      return await authFetch<ShareLinkPreviewResponse>(path, {}, clientIp);
    } catch (err) {
      if (!(err instanceof AuthError)) throw err;
    }
  }
  return apiFetch<ShareLinkPreviewResponse>(path, {}, clientIp);
}

// Landing page for shareable join links (`/join/{token}`). Unlike emailed invites, the link isn't tied to
// a person: signed-in users join (or request to join) as themselves, and meeting links also let someone
// without an account get an invite emailed to them.
export default async function ShareLinkPage({token}: { token: string }) {
  const viewer = await getSessionUser();

  let preview: ShareLinkPreviewResponse | null = null;
  let loadError: string | null = null;
  try {
    preview = await loadPreview(token, viewer != null);
  } catch (err) {
    if (!(err instanceof ApiError)) throw err;
    const body = err.body as Partial<ApiErrorBody> | null;
    loadError = body?.message ?? 'This link is invalid or has expired.';
  }

  if (preview && viewer && preview.viewerIsMember) {
    redirect(shareLinkDashboardPath(preview.kind, preview.groupId, preview.meetingId));
  }

  const selfPath = `/join/${encodeURIComponent(token)}`;

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
          {preview?.kind === 'MEETING' ? 'Meeting invite' : preview ? 'Group invite' : 'Invite link'}
        </h1>
        {preview ? (
          <ShareLinkActions
            token={token}
            preview={preview}
            signedIn={viewer != null}
            loginHref={loginHref(selfPath)}
          />
        ) : (
          <>
            <p className={authStyles.error} role='alert'>
              {loadError}
            </p>
            <Link className={authStyles.linkButton} href={viewer ? '/dashboard' : '/login'}>
              {viewer ? 'Go to your dashboard' : 'Go to login'}
            </Link>
          </>
        )}
      </div>
    </main>
  );
}
