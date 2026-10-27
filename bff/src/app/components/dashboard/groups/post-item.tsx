'use client';

import {useState} from 'react';
import {useRouter} from 'next/navigation';
import type {CommentsEnabledRequest, PostResponse} from '@/lib/types/group';
import {sendJson} from '@/lib/send-json';
import ConfirmButton from '@/app/components/dashboard/confirm-button';
import {formatDateTime} from '@/app/components/dashboard/home/format';
import type {GroupRole} from './load-group';
import PostComments from './post-comments';
import dashboardStyles from '@/app/components/dashboard/dashboard.module.css';
import styles from './groups.module.css';

interface PostItemProps {
  post: PostResponse;
  viewerId: number;
  role: GroupRole;
  timeZone: string;
  onDeleted: () => void;
}

// One post with its actions. Mirrors the server's rules: the author, admins, and the owner can delete
// posts and comments; only the owner can turn comments on or off.
export default function PostItem({post, viewerId, role, timeZone, onDeleted}: PostItemProps) {
  const router = useRouter();
  const [commentsEnabled, setCommentsEnabled] = useState(post.commentsEnabled);
  const [showComments, setShowComments] = useState(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const canModerate = role !== 'MEMBER';
  const postUrl = `/api/groups/${post.groupId}/posts/${post.id}`;

  // Runs one of the post's actions; resolves to whether it succeeded, showing the error otherwise.
  async function send(url: string, method: string, body?: object): Promise<boolean> {
    setBusy(true);
    setError(null);
    const result = await sendJson(url, method, body);
    setBusy(false);
    if (result.ok) {
      return true;
    }
    if (result.unauthenticated) {
      router.push('/login');
    } else {
      setError(result.message);
    }
    return false;
  }

  async function remove() {
    if (await send(postUrl, 'DELETE')) {
      onDeleted();
    }
  }

  async function toggleComments() {
    const body: CommentsEnabledRequest = {commentsEnabled: !commentsEnabled};
    if (await send(`${postUrl}/comments`, 'PATCH', body)) {
      setCommentsEnabled(body.commentsEnabled);
    }
  }

  return (
    <li className={styles.post}>
      <span className={dashboardStyles.meta}>
        <strong className={styles.author}>@{post.authorUsername}</strong>
        {' · '}
        <time dateTime={post.createdAt}>{formatDateTime(post.createdAt, timeZone)}</time>
      </span>
      <p className={styles.postBody}>{post.body}</p>
      {error && <span className={dashboardStyles.error} role='alert'>{error}</span>}
      <div className={styles.postActions}>
        <button type='button' className={dashboardStyles.buttonSecondary} aria-expanded={showComments}
                onClick={() => setShowComments((shown) => !shown)}>
          {showComments ? 'Hide comments' : 'Comments'}
        </button>
        {role === 'OWNER' && (
          <button type='button' className={dashboardStyles.buttonSecondary} disabled={busy}
                  onClick={toggleComments}>
            {commentsEnabled ? 'Turn off comments' : 'Turn on comments'}
          </button>
        )}
        {(canModerate || post.authorId === viewerId) && (
          <ConfirmButton label='Delete' confirmLabel='Delete post' disabled={busy} onConfirm={remove}/>
        )}
      </div>
      {showComments && (
        <PostComments postUrl={postUrl} commentsEnabled={commentsEnabled} viewerId={viewerId}
                      canModerate={canModerate} timeZone={timeZone}/>
      )}
    </li>
  );
}
