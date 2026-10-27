import Link from 'next/link';
import {verifySession} from '@/lib/dal';
import {loadMeeting, loadTimeZone, organizerOf} from '@/app/components/dashboard/meetings/load-meeting';
import {formatLongDate, formatMeetingTime} from '@/app/components/dashboard/home/format';
import dashboardStyles from '@/app/components/dashboard/dashboard.module.css';
import styles from '@/app/components/dashboard/meetings/meetings.module.css';

export default async function MeetingDetailsPage({params}: PageProps<'/dashboard/schedule/[meetingId]'>) {
  await verifySession();
  const {meetingId} = await params;
  const meeting = await loadMeeting(meetingId);
  const timeZone = await loadTimeZone();
  const {time} = formatMeetingTime(meeting.startTime!, meeting.endTime, timeZone);
  const organizer = organizerOf(meeting);
  const people = (meeting.participants?.length ?? 0) + (meeting.externalParticipantNames?.length ?? 0);

  return (
    <section className={dashboardStyles.card} aria-labelledby='details-title'>
      <div className={dashboardStyles.cardHeader}>
        <h2 id='details-title' className={dashboardStyles.cardTitle}>Details</h2>
      </div>
      <dl className={styles.details}>
        <div>
          <dt>When</dt>
          <dd>
            {formatLongDate(meeting.startTime!, timeZone)}, {time}
            <span className={dashboardStyles.meta}> ({timeZone.replaceAll('_', ' ')})</span>
          </dd>
        </div>
        <div>
          <dt>Where</dt>
          <dd>{meeting.location || <span className={dashboardStyles.meta}>No location set</span>}</dd>
        </div>
        <div>
          <dt>Group</dt>
          <dd>
            {meeting.groupId ? (
              <Link href={`/dashboard/groups/${meeting.groupId}/meetings`} className={dashboardStyles.cardLink}>
                {meeting.groupName ?? 'View group'}
              </Link>
            ) : <span className={dashboardStyles.meta}>Not in a group</span>}
          </dd>
        </div>
        <div>
          <dt>Organizer</dt>
          <dd>{organizer ? `@${organizer}` : <span className={dashboardStyles.meta}>Unknown</span>}</dd>
        </div>
        <div>
          <dt>People</dt>
          <dd>
            <Link href={`/dashboard/schedule/${meeting.id}/people`} className={dashboardStyles.cardLink}>
              {people} {people === 1 ? 'person' : 'people'}
            </Link>
          </dd>
        </div>
        <div>
          <dt>Visibility</dt>
          <dd>
            {meeting.isPrivate
              ? meeting.groupId ? 'Private: only participants and group admins can see it' : 'Private: only participants can see it'
              : meeting.groupId ? 'Visible to the group' : 'Visible to participants'}
          </dd>
        </div>
      </dl>
    </section>
  );
}
