'use client';

import {useState} from 'react';
import {useForm} from 'react-hook-form';
import {useRouter} from 'next/navigation';
import {zodResolver} from '@hookform/resolvers/zod';
import {z} from 'zod';
import type {CursorPage} from '@/lib/types/api';
import type {PostCreateRequest, PostResponse} from '@/lib/types/group';
import {sendJson} from '@/lib/send-json';
import {useCursorList} from '@/app/components/dashboard/use-cursor-list';
import type {GroupRole} from './load-group';
import PostItem from './post-item';
import authStyles from '@/app/components/auth/auth.module.css';
import dashboardStyles from '@/app/components/dashboard/dashboard.module.css';
import styles from './groups.module.css';

const postSchema = z.object({
  body: z.string().trim().min(1, 'Write something first').max(10000, 'Posts cannot exceed 10,000 characters'),
});
type PostFormData = z.infer<typeof postSchema>;

interface GroupPostsProps {
  groupId: number;
  initial: CursorPage<PostResponse> | null;
  viewerId: number;
  role: GroupRole;
  // Admins and the owner always can; members only when the group allows it.
  canPost: boolean;
  timeZone: string;
}

// Composer plus the group's posts, newest first.
export default function GroupPosts({groupId, initial, viewerId, role, canPost, timeZone}: GroupPostsProps) {
  const router = useRouter();
  const posts = useCursorList(`/api/groups/${groupId}/posts`, initial, 'Couldn’t load posts.');
  const [error, setError] = useState<string | null>(null);
  const {register, handleSubmit, reset, formState: {errors, isSubmitting}} = useForm<PostFormData>({
    resolver: zodResolver(postSchema),
    defaultValues: {body: ''},
  });

  const onSubmit = async (data: PostFormData) => {
    setError(null);
    // commentsEnabled omitted: the post follows the group's default.
    const body: PostCreateRequest = {body: data.body};
    const result = await sendJson<PostResponse>(`/api/groups/${groupId}/posts`, 'POST', body);
    if (!result.ok) {
      if (result.unauthenticated) {
        router.push('/login');
        return;
      }
      setError(result.message);
      return;
    }
    reset();
    posts.setItems((current) => [result.data, ...current]);
  };

  return (
    <>
      {canPost && (
        <section className={dashboardStyles.card}>
          <form onSubmit={handleSubmit(onSubmit)} className={authStyles.form} noValidate>
            <label className={authStyles.field}>
              <span className={authStyles.label}>New post</span>
              <textarea rows={3} maxLength={10000} placeholder='Share something with the group'
                        {...register('body')} className={styles.textarea}/>
              {errors.body && <span className={authStyles.error} role='alert'>{errors.body.message}</span>}
            </label>
            {error && <p className={authStyles.error} role='alert'>{error}</p>}
            <div className={styles.formFooter}>
              <button type='submit' className={dashboardStyles.button} disabled={isSubmitting}>
                {isSubmitting ? 'Posting…' : 'Post'}
              </button>
            </div>
          </form>
        </section>
      )}
      <section className={dashboardStyles.card}>
        {!canPost && <p className={dashboardStyles.meta}>Only admins can post in this group.</p>}
        {posts.loaded && posts.items.length === 0 ? (
          <p className={dashboardStyles.empty}>No posts yet. Start the conversation.</p>
        ) : (
          <ul className={styles.groupList}>
            {posts.items.map((post) => (
              <PostItem key={post.id} post={post} viewerId={viewerId} role={role} timeZone={timeZone}
                        onDeleted={() => posts.setItems((current) => current.filter((p) => p.id !== post.id))}/>
            ))}
          </ul>
        )}
        {posts.error && <p className={dashboardStyles.error}>{posts.error}</p>}
        {posts.hasMore && (
          <div className={styles.loadMore}>
            <button type='button' className={dashboardStyles.buttonSecondary} disabled={posts.loading}
                    onClick={posts.loadMore}>
              {posts.loading ? 'Loading…' : 'Load more'}
            </button>
          </div>
        )}
      </section>
    </>
  );
}
