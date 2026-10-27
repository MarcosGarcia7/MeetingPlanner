import type {GroupSummaryResponse} from '@/lib/types/group';
import GroupCard from './group-card';
import dashboardStyles from '@/app/components/dashboard/dashboard.module.css';
import styles from './groups.module.css';

interface GroupShelfProps {
  title: string;
  groups: GroupSummaryResponse[] | null;
  empty: string;
}

// One titled card of groups; `groups` is null when its request failed.
export default function GroupShelf({title, groups, empty}: GroupShelfProps) {
  return (
    <section className={dashboardStyles.card}>
      <div className={dashboardStyles.cardHeader}>
        <h2 className={dashboardStyles.cardTitle}>{title}</h2>
      </div>
      {groups === null ? (
        <p className={dashboardStyles.error}>Couldn’t load these groups.</p>
      ) : groups.length === 0 ? (
        <p className={dashboardStyles.empty}>{empty}</p>
      ) : (
        <ul className={styles.groupList}>
          {groups.map((group) => <GroupCard key={group.id} group={group}/>)}
        </ul>
      )}
    </section>
  );
}
