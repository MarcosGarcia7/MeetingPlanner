'use client';

import Link from 'next/link';
import {usePathname} from 'next/navigation';
import clsx from 'clsx';
import styles from './dashboard.module.css';

export interface Tab {
  label: string;
  href: string;
}

// Sub-navigation for a dashboard section. Each tab is its own route; the one matching the path exactly
// is current.
export default function Tabs({tabs, label}: { tabs: Tab[]; label: string }) {
  const pathname = usePathname();

  return (
    <nav className={styles.tabs} aria-label={label}>
      {tabs.map((tab) => {
        const current = pathname === tab.href;
        return (
          <Link key={tab.href} href={tab.href} aria-current={current ? 'page' : undefined}
                className={clsx(styles.tab, {[styles.currentTab]: current})}>
            {tab.label}
          </Link>
        );
      })}
    </nav>
  );
}
