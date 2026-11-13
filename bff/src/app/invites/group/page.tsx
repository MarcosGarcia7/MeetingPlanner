import InviteLinkPage from '@/app/components/invite-link/invite-link-page';

export default async function GroupInviteLinkPage({searchParams}: PageProps<'/invites/group'>) {
  const {token} = await searchParams;
  return <InviteLinkPage kind='group' token={token}/>;
}
