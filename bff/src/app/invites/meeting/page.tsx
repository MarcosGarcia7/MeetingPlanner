import InviteLinkPage from '@/app/components/invite-link/invite-link-page';

export default async function MeetingInviteLinkPage({searchParams}: PageProps<'/invites/meeting'>) {
  const {token} = await searchParams;
  return <InviteLinkPage kind='meeting' token={token}/>;
}
