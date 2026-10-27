import {headers} from 'next/headers';
import {redirect} from 'next/navigation';
import {verifySession} from '@/lib/dal';
import {AuthError, authFetch, getClientIp} from '@/lib/api-client';
import type {CursorPage} from '@/lib/types/api';
import type {GroupSummaryResponse} from '@/lib/types/group';
import MyGroups from '@/app/components/dashboard/groups/my-groups';

export default async function MyGroupsPage({searchParams}: PageProps<'/dashboard/groups/mine'>) {
  const {userId} = await verifySession();
  const clientIp = getClientIp(await headers());
  const {new: creating} = await searchParams;

  let initial: CursorPage<GroupSummaryResponse> | null = null;
  try {
    initial = await authFetch<CursorPage<GroupSummaryResponse>>('/api/groups?size=20', {}, clientIp);
  } catch (error) {
    if (error instanceof AuthError) {
      redirect('/login');
    }
  }

  return <MyGroups initial={initial} viewerId={userId} creating={creating === '1'}/>;
}
