import Link from 'next/link';
import {headers} from 'next/headers';
import {redirect} from 'next/navigation';
import {verifySession} from '@/lib/dal';
import {AuthError, authFetch, getClientIp} from '@/lib/api-client';
import type {TimeChangeProposalResponse} from '@/lib/types/meeting';
import {loadMeeting, loadTimeZone} from '@/app/components/dashboard/meetings/load-meeting';
import MeetingHeader from '@/app/components/dashboard/meetings/meeting-header';
import TimeChangeBanner from '@/app/components/dashboard/meetings/time-change-banner';
import Tabs from '@/app/components/dashboard/tabs';
import dashboardStyles from '@/app/components/dashboard/dashboard.module.css';

// A single meeting: header, any pending time change, then Details | People (| Manage for its creator and
// its group's owner and admins).
export default async function MeetingLayout({children, params}: LayoutProps<'/dashboard/schedule/[meetingId]'>) {
  const {userId} = await verifySession();
  const {meetingId} = await params;
  const meeting = await loadMeeting(meetingId);
  const clientIp = getClientIp(await headers());
  const get = <T, >(path: string) => authFetch<T>(path, {}, clientIp);
  const base = `/dashboard/schedule/${meeting.id}`;

  const timeZone = await loadTimeZone();
  // Fetched for the viewer's vote. A failure (or the proposal resolving meanwhile) just hides the banner.
  let proposal: TimeChangeProposalResponse | null = null;
  if (meeting.pendingTimeChange) {
    try {
      proposal = await get<TimeChangeProposalResponse>(`/api/meetings/${meeting.id}/time-change`);
    } catch (error) {
      if (error instanceof AuthError) {
        redirect('/login');
      }
    }
  }

  return (
    <main className={dashboardStyles.page}>
      <Link href='/dashboard/schedule/meetings' className={dashboardStyles.cardLink}>← Meetings</Link>
      <MeetingHeader meeting={meeting} viewerId={userId} timeZone={timeZone}/>
      {proposal && <TimeChangeBanner proposal={proposal} viewerId={userId} timeZone={timeZone}/>}
      <Tabs label={`${meeting.title} sections`} tabs={[
        {label: 'Details', href: base},
        {label: 'People', href: `${base}/people`},
        ...(meeting.canManage ? [{label: 'Manage', href: `${base}/manage`}] : []),
      ]}/>
      {children}
    </main>
  );
}
