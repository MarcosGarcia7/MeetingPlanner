import styles from './dashboard.module.css';

export default function CountBadge({count, label}: { count: number; label: string }) {
  if (count <= 0) return null;
  return (
    <span className={styles.badge} aria-label={`${count} ${label}`}>
      {count > 99 ? '99+' : count}
    </span>
  );
}
