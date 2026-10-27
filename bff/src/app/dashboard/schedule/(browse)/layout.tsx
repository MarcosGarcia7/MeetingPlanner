import Tabs from '@/app/components/dashboard/tabs';
import dashboardStyles from '@/app/components/dashboard/dashboard.module.css';

const tabs = [
  {label: 'Overview', href: '/dashboard/schedule'},
  {label: 'Meetings', href: '/dashboard/schedule/meetings'},
  {label: 'Availability', href: '/dashboard/schedule/availability'},
];

export default function ScheduleLayout({children}: { children: React.ReactNode }) {
  return (
    <main className={dashboardStyles.page}>
      <Tabs tabs={tabs} label='Schedule'/>
      {children}
    </main>
  );
}
