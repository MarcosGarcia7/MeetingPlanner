import Link from 'next/link';
import type {MeetingSummaryResponse} from '@/lib/types/meeting';
import MeetingListItem from './meeting-list-item';
import dashboardStyles from '@/app/components/dashboard/dashboard.module.css';
import styles from './home.module.css';

interface UpcomingMeetingsProps {
  meetings: MeetingSummaryResponse[] | null;
  timeZone?: string;
}

export default function UpcomingMeetings({meetings, timeZone}: UpcomingMeetingsProps) {
  return (
    <section className={dashboardStyles.card}>
      <div className={dashboardStyles.cardHeader}>
        <h2 className={dashboardStyles.cardTitle}>Upcoming meetings</h2>
      </div>
      {meetings === null ? (
        <p className={dashboardStyles.error}>Couldn’t load your meetings.</p>
      ) : meetings.length === 0 ? (
        <p className={dashboardStyles.empty}>
          Nothing scheduled. <Link href='/dashboard/schedule' className={dashboardStyles.cardLink}>Plan one</Link>
        </p>
      ) : (
        <ul className={styles.cardList}>
          {meetings.map((meeting) => <MeetingListItem key={meeting.id} meeting={meeting} timeZone={timeZone}/>)}
        </ul>
      )}
    </section>
  );
}
