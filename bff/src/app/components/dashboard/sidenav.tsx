import NavLinks from '@/app/components/dashboard/nav-links';
import styles from './sidenav.module.css';


export default function SideNav({unread}: { unread: number }) {
  return (
    <div className={styles.sidenav}>
      <NavLinks unread={unread}/>
    </div>
  )
}
