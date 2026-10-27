import dashboardStyles from '@/app/components/dashboard/dashboard.module.css';
import styles from './friends.module.css';

interface LoadMoreProps {
  list: { hasMore: boolean; loading: boolean; error: string | null; loadMore: () => void };
}

// Footer for a useCursorList-backed card: the load error, then a Load more button while pages remain.
export default function LoadMore({list}: LoadMoreProps) {
  return (
    <>
      {list.error && <p className={dashboardStyles.error}>{list.error}</p>}
      {list.hasMore && (
        <div className={styles.loadMore}>
          <button type='button' className={dashboardStyles.buttonSecondary} disabled={list.loading}
                  onClick={list.loadMore}>
            {list.loading ? 'Loading…' : 'Load more'}
          </button>
        </div>
      )}
    </>
  );
}
