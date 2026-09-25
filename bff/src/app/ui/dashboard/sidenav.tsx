import Link from 'next/link';
import NavLinks from '@/app/ui/dashboard/nav-links';
import styles from './sidenav.module.css';


export default function SideNav() {
    return (
        <div className={styles.sidenav}>
            <NavLinks/>
        </div>
    )
}