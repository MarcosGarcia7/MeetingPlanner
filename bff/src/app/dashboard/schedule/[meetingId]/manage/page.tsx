import {notFound} from 'next/navigation';
import {verifySession} from '@/lib/dal';
import {loadMeeting, loadTimeZone} from '@/app/components/dashboard/meetings/load-meeting';
import MeetingEditForm from '@/app/components/dashboard/meetings/meeting-edit-form';
import DeleteMeeting from '@/app/components/dashboard/meetings/delete-meeting';
import type {MeetingResponse} from "@/lib/types/meeting";

function hasEnded(meeting: MeetingResponse): boolean {
  const end = meeting.endTime ?? meeting.startTime;
  return end !== null && Date.parse(end) < Date.now();
}

// Manage tab, for the creator and the group's owner and admins: edit and delete. People management
// (invites, removals, join requests) is on the People tab.
export default async function MeetingManagePage({params}: PageProps<'/dashboard/schedule/[meetingId]/manage'>) {
  const {userId} = await verifySession();
  const {meetingId} = await params;
  const meeting = await loadMeeting(meetingId);
  const ended = hasEnded(meeting);
  if (!meeting.canManage) {
    notFound();
  }

  return (
    <>
      <MeetingEditForm meeting={meeting} viewerId={userId} timeZone={await loadTimeZone()} ended={ended}/>
      <DeleteMeeting meetingId={meeting.id!} ended={ended}/>
    </>
  );
}
