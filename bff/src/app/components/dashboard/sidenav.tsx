import NavLinks from '@/app/components/dashboard/nav-links';
import styles from './sidenav.module.css';


export default function SideNav() {
  return (
    <div className={styles.sidenav}>
      <NavLinks/>
    </div>
  )
}
