import Link from 'next/link';
import type {MeetingSummaryResponse} from '@/lib/types/meeting';
import {formatMeetingTime} from './format';
import dashboardStyles from '@/app/components/dashboard/dashboard.module.css';
import styles from './home.module.css';
import meetingStyles from './upcoming-meetings.module.css';

interface MeetingListItemProps {
  meeting: MeetingSummaryResponse;
  timeZone?: string;
  // Off inside a group's own page, where the group name is redundant.
  showGroup?: boolean;
}

// One meeting row (date chip, title, time, details) linking to the meeting; render inside a styles.cardList.
export default function MeetingListItem({meeting, timeZone, showGroup = true}: MeetingListItemProps) {
  const {day, time} = formatMeetingTime(meeting.startTime, meeting.endTime, timeZone);
  const details = [
    showGroup && meeting.groupName,
    meeting.location,
    `${meeting.participantCount} ${meeting.participantCount === 1 ? 'person' : 'people'}`,
  ].filter(Boolean).join(' · ');
  return (
    <li>
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
}
