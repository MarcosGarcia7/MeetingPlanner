import {verifySession} from '@/lib/dal';
import type {GroupMemberResponse, GroupResponse} from '@/lib/types/group';
import {loadGroup, roleIn} from '@/app/components/dashboard/groups/load-group';
import MemberList from '@/app/components/dashboard/groups/member-list';

// Owner first, then admins, then everyone else, each alphabetically.
function rank(group: GroupResponse, member: GroupMemberResponse) {
  return member.userId === group.ownerId ? 0 : member.isAdmin ? 1 : 2;
}

export default async function GroupMembersPage({params}: PageProps<'/dashboard/groups/[groupId]/members'>) {
  const {userId} = await verifySession();
  const {groupId} = await params;
  const group = await loadGroup(groupId);
  const role = roleIn(group, userId);
  if (!role) {
    return null;
  }
  const members = [...(group.members ?? [])].sort((a, b) =>
    rank(group, a) - rank(group, b) || (a.username ?? '').localeCompare(b.username ?? ''));

  return (
    <MemberList groupId={Number(groupId)} ownerId={group.ownerId} members={members} viewerId={userId}
                viewerRole={role}/>
  );
}
