import Link from 'next/link';
import {verifySession} from '@/lib/dal';
import {loadGroup, roleIn} from '@/app/components/dashboard/groups/load-group';
import GroupHeader from '@/app/components/dashboard/groups/group-header';
import Tabs from '@/app/components/dashboard/tabs';
import dashboardStyles from '@/app/components/dashboard/dashboard.module.css';

// A single group: header, then Posts | Meetings | Members (| Manage for admins and the owner).
// Non-members only see the header.
export default async function GroupLayout({children, params}: LayoutProps<'/dashboard/groups/[groupId]'>) {
  const {userId} = await verifySession();
  const {groupId} = await params;
  const group = await loadGroup(groupId);
  const role = roleIn(group, userId);
  const base = `/dashboard/groups/${group.id}`;

  return (
    <main className={dashboardStyles.page}>
      <Link href='/dashboard/groups/mine' className={dashboardStyles.cardLink}>← My groups</Link>
      <GroupHeader group={group} role={role}/>
      {role ? (
        <>
          <Tabs label={`${group.groupName} sections`} tabs={[
            {label: 'Posts', href: base},
            {label: 'Meetings', href: `${base}/meetings`},
            {label: 'Members', href: `${base}/members`},
            ...(role === 'MEMBER' ? [] : [{label: 'Manage', href: `${base}/manage`}]),
          ]}/>
          {children}
        </>
      ) : (
        <section className={dashboardStyles.card}>
          <p className={dashboardStyles.empty}>
            Join this group to see its posts, meetings, and members.{' '}
            <Link href='/dashboard/groups/discover' className={dashboardStyles.cardLink}>Find it in Discover</Link>
          </p>
        </section>
      )}
    </main>
  );
}
