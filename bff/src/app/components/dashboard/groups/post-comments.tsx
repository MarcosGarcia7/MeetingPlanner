'use client';

import {useEffect, useState} from 'react';
import {useForm} from 'react-hook-form';
import {useRouter} from 'next/navigation';
import {zodResolver} from '@hookform/resolvers/zod';
import {z} from 'zod';
import type {CursorPage} from '@/lib/types/api';
import type {PostCommentCreateRequest, PostCommentResponse} from '@/lib/types/group';
import {sendJson} from '@/lib/send-json';
import {useCursorList} from '@/app/components/dashboard/use-cursor-list';
import {formatDateTime} from '@/app/components/dashboard/home/format';
import authStyles from '@/app/components/auth/auth.module.css';
import dashboardStyles from '@/app/components/dashboard/dashboard.module.css';
import styles from './groups.module.css';

interface PostCommentsProps {
  // BFF route of the post, e.g. /api/groups/1/posts/2.
  postUrl: string;
  commentsEnabled: boolean;
  viewerId: number;
  canModerate: boolean;
  timeZone: string;
}

type ThreadProps = Omit<PostCommentsProps, 'postUrl'> & {
  commentsUrl: string;
  initial: CursorPage<PostCommentResponse>;
};

// Loads a post's first page of comments when it's opened, then shows the thread.
export default function PostComments({postUrl, ...props}: PostCommentsProps) {
  const router = useRouter();
  const commentsUrl = `${postUrl}/comments`;
  const [page, setPage] = useState<CursorPage<PostCommentResponse> | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    sendJson<CursorPage<PostCommentResponse>>(commentsUrl).then((result) => {
      if (cancelled) {
        return;
      }
      if (result.ok) {
        setPage(result.data);
      } else if (result.unauthenticated) {
        router.push('/login');
      } else {
        setError(result.message);
      }
    });
    return () => {
      cancelled = true;
    };
  }, [commentsUrl, router]);

  if (error) {
    return <p className={dashboardStyles.error}>{error}</p>;
  }
  if (!page) {
    return <p className={dashboardStyles.meta}>Loading comments…</p>;
  }
  return <CommentThread commentsUrl={commentsUrl} initial={page} {...props}/>;
}

const commentSchema = z.object({
  body: z.string().trim().min(1, 'Write a comment first').max(2000, 'Comments cannot exceed 2,000 characters'),
});
type CommentFormData = z.infer<typeof commentSchema>;

// Comments oldest first, a page at a time, with the reply box at the bottom.
function CommentThread({commentsUrl, initial, commentsEnabled, viewerId, canModerate, timeZone}: ThreadProps) {
  const router = useRouter();
  const comments = useCursorList(commentsUrl, initial, 'Couldn’t load comments.');
  const [error, setError] = useState<string | null>(null);
  const [deleting, setDeleting] = useState<number | null>(null);
  const {register, handleSubmit, reset, formState: {errors, isSubmitting}} = useForm<CommentFormData>({
    resolver: zodResolver(commentSchema),
    defaultValues: {body: ''},
  });

  function fail(result: { message: string; unauthenticated: boolean }) {
    if (result.unauthenticated) {
      router.push('/login');
    } else {
      setError(result.message);
    }
  }

  const onSubmit = async (data: CommentFormData) => {
    setError(null);
    const body: PostCommentCreateRequest = {body: data.body};
    const result = await sendJson<PostCommentResponse>(commentsUrl, 'POST', body);
    if (!result.ok) {
      fail(result);
      return;
    }
    reset();
    // With older pages still unloaded, the new comment arrives with the last page instead.
    if (!comments.hasMore) {
      comments.setItems((current) => [...current, result.data]);
    }
  };

  async function remove(commentId: number) {
    setDeleting(commentId);
    setError(null);
    const result = await sendJson(`${commentsUrl}/${commentId}`, 'DELETE');
    setDeleting(null);
    if (!result.ok) {
      fail(result);
      return;
    }
    comments.setItems((current) => current.filter((comment) => comment.id !== commentId));
  }

  return (
    <div className={styles.comments}>
      {comments.items.length === 0 && !comments.hasMore && (
        <p className={dashboardStyles.meta}>No comments yet.</p>
      )}
      <ul className={styles.commentList}>
        {comments.items.map((comment) => (
          <li key={comment.id} className={styles.comment}>
            <div className={styles.groupBody}>
              <span className={dashboardStyles.meta}>
                <strong className={styles.author}>@{comment.authorUsername}</strong>
                {' · '}
                <time dateTime={comment.createdAt}>{formatDateTime(comment.createdAt, timeZone)}</time>
              </span>
              <p className={styles.commentBody}>{comment.body}</p>
            </div>
            {(canModerate || comment.authorId === viewerId) && (
              <button type='button' className={dashboardStyles.buttonSecondary}
                      disabled={deleting === comment.id} onClick={() => remove(comment.id)}>
                Delete
              </button>
            )}
          </li>
        ))}
      </ul>
      {comments.hasMore && (
        <button type='button' className={dashboardStyles.buttonSecondary} disabled={comments.loading}
                onClick={comments.loadMore}>
          {comments.loading ? 'Loading…' : 'More comments'}
        </button>
      )}
      {comments.error && <p className={dashboardStyles.error}>{comments.error}</p>}
      {error && <p className={dashboardStyles.error} role='alert'>{error}</p>}
      {commentsEnabled ? (
        <form onSubmit={handleSubmit(onSubmit)} className={styles.commentForm} noValidate>
          <input type='text' maxLength={2000} placeholder='Write a comment' aria-label='Write a comment'
                 {...register('body')} className={authStyles.input}/>
          <button type='submit' className={dashboardStyles.button} disabled={isSubmitting}>Reply</button>
          {errors.body && <span className={authStyles.error} role='alert'>{errors.body.message}</span>}
        </form>
      ) : (
        <p className={dashboardStyles.meta}>Comments are turned off for this post.</p>
      )}
    </div>
  );
}
