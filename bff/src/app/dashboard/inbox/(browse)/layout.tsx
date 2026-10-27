import Tabs from '@/app/components/dashboard/tabs';
import dashboardStyles from '@/app/components/dashboard/dashboard.module.css';

const tabs = [
  {label: 'Overview', href: '/dashboard/inbox'},
  {label: 'Invites', href: '/dashboard/inbox/invites'},
  {label: 'Requests', href: '/dashboard/inbox/requests'},
  {label: 'Proposals', href: '/dashboard/inbox/proposals'},
];

export default function InboxLayout({children}: { children: React.ReactNode }) {
  return (
    <main className={dashboardStyles.page}>
      <Tabs tabs={tabs} label='Inbox'/>
      {children}
    </main>
  );
}
