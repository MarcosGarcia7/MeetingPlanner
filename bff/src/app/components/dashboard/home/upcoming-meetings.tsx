import Link from 'next/link';
import type {MeetingSummaryResponse} from '@/lib/types/meeting';
import {formatMeetingTime} from './format';
import dashboardStyles from '@/app/components/dashboard/dashboard.module.css';
import styles from './home.module.css';
import meetingStyles from './upcoming-meetings.module.css';

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
          {meetings.map((meeting) => {
            const {day, time} = formatMeetingTime(meeting.startTime, meeting.endTime, timeZone);
            const details = [
              meeting.groupName,
              meeting.location,
              `${meeting.participantCount} ${meeting.participantCount === 1 ? 'person' : 'people'}`,
            ].filter(Boolean).join(' · ');
            return (
              <li key={meeting.id}>
                <Link href={`/dashboard/schedule?meeting=${meeting.id}`} className={styles.itemCard}>
                  <time dateTime={meeting.startTime} className={meetingStyles.date}>{day}</time>
                  <div className={styles.itemBody}>
                    <span className={styles.itemTitle}>{meeting.title}</span>
                    <span className={dashboardStyles.meta}>{time}</span>
                    <span className={dashboardStyles.meta}>{details}</span>
                  </div>
                </Link>
              </li>
            );
          })}
        </ul>
      )}
    </section>
  );
}
