import Tabs from '@/app/components/dashboard/tabs';
import dashboardStyles from '@/app/components/dashboard/dashboard.module.css';

const tabs = [
  {label: 'Overview', href: '/dashboard/groups'},
  {label: 'My groups', href: '/dashboard/groups/mine'},
  {label: 'Discover', href: '/dashboard/groups/discover'},
];

export default function GroupsLayout({children}: { children: React.ReactNode }) {
  return (
    <main className={dashboardStyles.page}>
      <Tabs tabs={tabs} label='Groups'/>
      {children}
    </main>
  );
}
