import SideNav from '@/app/ui/dashboard/sidenav';
import styles from '@/app/ui/dashboard/dashboard.module.css';

export default function Layout({children}: { children: React.ReactNode }) {
    return (
        <div className={styles.layoutContainer}>
            <div className={styles.navComponent}>
                <SideNav/>
            </div>
            <div className={styles.mainContent}>
                {children}
            </div>
        </div>
    )
}