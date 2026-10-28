import type {Metadata} from 'next';
import Link from 'next/link';
import {
  BellAlertIcon,
  CalendarDaysIcon,
  EnvelopeOpenIcon,
  ExclamationTriangleIcon,
  HandRaisedIcon,
  ShieldCheckIcon,
  UserGroupIcon,
  UserPlusIcon,
} from '@heroicons/react/24/outline';
import headerStyles from './header.module.css';
import styles from './page.module.css';

export const metadata: Metadata = {
  title: 'Meeting Planner — find a time that works for everyone',
  description:
    'Plan meetings with your friends and groups. Conflicts are flagged against everyone\'s schedule, and time changes are put to a vote.',
};

const steps = [
  {
    title: 'Join a group',
    body: 'Add friends and create groups for your team, club, or family. Invite people by name or share a link.',
  },
  {
    title: 'Propose a meeting',
    body: 'Pick a time and invite people. Meeting Planner checks everyone\'s schedule and flags conflicts before you send.',
  },
  {
    title: 'Agree on the details',
    body: 'Attendees accept invites, others can ask to join, and anyone can propose a new time for the group to vote on.',
  },
];

const features = [
  {
    icon: CalendarDaysIcon,
    title: 'Conflict-aware scheduling',
    body: 'See who is double-booked before a meeting is set, not after.',
  },
  {
    icon: HandRaisedIcon,
    title: 'Time-change voting',
    body: 'Need to move a meeting? Propose a new time and let attendees vote on it.',
  },
  {
    icon: UserGroupIcon,
    title: 'Groups',
    body: 'Organize people into groups, share schedules within them, and plan group meetings in one place.',
  },
  {
    icon: UserPlusIcon,
    title: 'Friends',
    body: 'Connect with the people you meet with most, and block anyone you\'d rather not hear from.',
  },
  {
    icon: EnvelopeOpenIcon,
    title: 'Invites & join requests',
    body: 'Invite people directly or with a link, and approve requests from group members who want to join.',
  },
  {
    icon: BellAlertIcon,
    title: 'Notifications & email',
    body: 'Get notified about invites, votes, and upcoming meetings, by email if you prefer. You choose what you hear about.',
  },
];

export default function Home() {
  return (
    <div className={styles.page}>
      <main className={styles.main}>
        <section className={styles.hero}>
          <div className={styles.heroText}>
            <h2 className={styles.headline}>Find a time that works for everyone.</h2>
            <p className={styles.lede}>
              Meeting Planner brings your friends, groups, and schedules together, flags conflicts before they
              happen, and lets everyone vote when plans need to change.
            </p>
            <div className={styles.actions}>
              <Link className={headerStyles.buttonPrimary} href='/signup'>Get started</Link>
              <Link className={headerStyles.buttonSecondary} href='/login'>Log in</Link>
            </div>
          </div>
        </section>

        <section className={styles.section} aria-labelledby='how-title'>
          <h2 id='how-title' className={styles.sectionTitle}>How to use Meeting Planner</h2>
          <ol className={styles.steps}>
            {steps.map((step, i) => (
              <li key={step.title} className={styles.step}>
                <span className={styles.stepNumber}>{i + 1}</span>
                <h3 className={styles.cardTitle}>{step.title}</h3>
                <p className={styles.cardBody}>{step.body}</p>
              </li>
            ))}
          </ol>
        </section>

        <section className={styles.section} aria-labelledby='features-title'>
          <h2 id='features-title' className={styles.sectionTitle}>Everything you need to plan a meeting</h2>
          <ul className={styles.features}>
            {features.map(({icon: Icon, title, body}) => (
              <li key={title} className={styles.card}>
                <Icon className={styles.featureIcon} aria-hidden='true'/>
                <h3 className={styles.cardTitle}>{title}</h3>
                <p className={styles.cardBody}>{body}</p>
              </li>
            ))}
          </ul>
        </section>

        <section className={styles.section} aria-labelledby='privacy-title'>
          <div className={styles.privacy}>
            <ShieldCheckIcon className={styles.privacyIcon} aria-hidden='true'/>
            <div>
              <h2 id='privacy-title' className={styles.cardTitle}>Your schedule, your call</h2>
              <p className={styles.cardBody}>
                Private meetings are invite-only. Make your account private, choose whether friends and groups can
                see your schedule, and block people at any time. Every account is verified by email.
              </p>
            </div>
          </div>
        </section>
      </main>

      <footer className={styles.footer}>
        © {new Date().getFullYear()} Meeting Planner
      </footer>
    </div>
  );
}
