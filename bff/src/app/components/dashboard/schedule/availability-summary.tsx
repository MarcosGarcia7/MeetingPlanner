import Link from 'next/link';
import clsx from 'clsx';
import type {UserAvailabilityResponse} from '@/lib/types/user';
import {DAY_LABELS, DAYS, formatWindow, sortWindows} from './availability';
import dashboardStyles from '@/app/components/dashboard/dashboard.module.css';
import styles from './schedule.module.css';

interface AvailabilitySummaryProps {
  // Undefined when the request failed; null when availability was never saved.
  availability: UserAvailabilityResponse | null | undefined;
  timeZone: string;
}

// The week's hours at a glance, with today marked, or a nudge to set them. A server component: "today"
// depends on the current time, so it must not render again on the client.
export default function AvailabilitySummary({availability, timeZone}: AvailabilitySummaryProps) {
  const today = new Intl.DateTimeFormat('en-US', {weekday: 'long', timeZone}).format(new Date()).toLowerCase();

  return (
    <section className={dashboardStyles.card}>
      <div className={dashboardStyles.cardHeader}>
        <h2 className={dashboardStyles.cardTitle}>Weekly availability</h2>
      </div>
      {availability === undefined ? (
        <p className={dashboardStyles.error}>Couldn’t load your availability.</p>
      ) : !availability?.availabilitySet ? (
        <p className={dashboardStyles.empty}>
          Set the hours you’re usually free, and people scheduling with you will be warned about times outside them.
        </p>
      ) : (
        <dl className={styles.weekList}>
          {DAYS.map((day) => {
            const windows = sortWindows(availability[day]);
            return (
              <div key={day} className={clsx(styles.weekDay, {[styles.today]: day === today})}>
                <dt>{DAY_LABELS[day].slice(0, 3)}</dt>
                <dd>{windows.length === 0 ? 'Unavailable' : windows.map(formatWindow).join(', ')}</dd>
              </div>
            );
          })}
        </dl>
      )}
      {availability?.availabilitySet && (
        <p className={dashboardStyles.meta}>Times are in {timeZone.replaceAll('_', ' ')}.</p>
      )}
      <div className={dashboardStyles.cardFooter}>
        <Link href='/dashboard/schedule/availability' className={dashboardStyles.cardLink}>
          {availability?.availabilitySet ? 'Edit availability' : 'Set your availability'}
        </Link>
      </div>
    </section>
  );
}
