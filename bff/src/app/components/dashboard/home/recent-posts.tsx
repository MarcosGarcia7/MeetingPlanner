import Link from 'next/link';
import type {CursorPage} from '@/lib/types/api';
import type {PostResponse} from '@/lib/types/group';
import {formatRelative} from './format';
import styles from './home.module.css';
import postStyles from './recent-posts.module.css';

export default function RecentPosts({posts}: { posts: CursorPage<PostResponse> | null }) {
  return (
    <section className={styles.card}>
      <div className={styles.cardHeader}>
        <h2 className={styles.cardTitle}>Recent posts</h2>
      </div>
      {posts === null ? (
        <p className={styles.error}>Couldn’t load recent posts.</p>
      ) : posts.items.length === 0 ? (
        <p className={styles.empty}>No posts in your groups yet.</p>
      ) : (
        <ul className={styles.cardList}>
          {posts.items.map((post) => (
            <li key={post.id}>
              <Link href={`/dashboard/groups?group=${post.groupId}`} className={styles.itemCard}>
                <div className={styles.itemBody}>
                  <span className={styles.meta}>
                    <strong className={postStyles.author}>@{post.authorUsername}</strong>
                    {post.groupName && <> in {post.groupName}</>}
                    {' · '}
                    <time dateTime={post.createdAt}>{formatRelative(post.createdAt)}</time>
                  </span>
                  <p className={postStyles.body}>{post.body}</p>
                </div>
              </Link>
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}
