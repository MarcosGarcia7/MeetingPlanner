import Tabs from '@/app/components/dashboard/tabs';
import styles from '@/app/components/dashboard/settings/settings.module.css';

const tabs = [
  {label: 'Notifications', href: '/dashboard/settings'},
  {label: 'Privacy', href: '/dashboard/settings/privacy'},
  {label: 'Account', href: '/dashboard/settings/account'},
];

export default function SettingsLayout({children}: { children: React.ReactNode }) {
  return (
    <main className={styles.page}>
      <Tabs tabs={tabs} label='Settings'/>
      {children}
    </main>
  );
}
