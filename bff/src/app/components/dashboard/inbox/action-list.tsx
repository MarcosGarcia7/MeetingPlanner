'use client';

import clsx from 'clsx';
import type {ScheduleConflict, TimeChangeProposalResponse} from '@/lib/types/meeting';
import {usePostAction} from '@/app/components/dashboard/use-post-action';
import TimeChangeVote from '@/app/components/dashboard/meetings/time-change-vote';
import InviteResponseActions from '@/app/components/dashboard/meetings/invite-response';
import dashboardStyles from '@/app/components/dashboard/dashboard.module.css';
import homeStyles from '@/app/components/dashboard/home/home.module.css';
import styles from './inbox.module.css';

export interface InboxAction {
  label: string;
  url: string;
  method?: string;
  body?: object;
  secondary?: boolean;
}

// One pending invite, request, or proposal. Pages build these on the server; the buttons call BFF routes.
export interface InboxItem {
  key: string;
  // "Group", "Meeting", "Friend".
  kind: string;
  title: React.ReactNode;
  details?: string[];
  warnings?: string[];
  actions: InboxAction[];
  // A time change to vote on: rendered with TimeChangeVote (which handles clashes) after the actions.
  vote?: {proposal: TimeChangeProposalResponse; timeZone: string};
  // A meeting invite to answer: rendered with InviteResponseActions (which handles clashes) after the actions.
  invite?: {inviteId: number; conflicts: ScheduleConflict[]; timeZone: string};
}

interface ActionListProps {
  title: string;
  // Null when the request behind the list failed.
  items: InboxItem[] | null;
  empty: string;
  // Some items exist past the ones loaded.
  truncated?: boolean;
}

export default function ActionList({title, items, empty, truncated}: ActionListProps) {
  const {run, busy, errorFor} = usePostAction();

  return (
    <section className={dashboardStyles.card}>
      <div className={styles.cardHeader}>
        <h2 className={dashboardStyles.cardTitle}>{title}</h2>
      </div>
      {items === null ? (
        <p className={dashboardStyles.error}>Couldn’t load this list.</p>
      ) : items.length === 0 ? (
        <p className={dashboardStyles.empty}>{empty}</p>
      ) : (
        <ul className={homeStyles.cardList}>
          {items.map((item) => {
            const error = errorFor(item.key);
            return (
              <li key={item.key} className={dashboardStyles.itemRow}>
                <div className={homeStyles.itemBody}>
                  <span className={clsx(homeStyles.itemTitle, styles.rowTitle)}>
                    <span className={styles.chip}>{item.kind}</span>
                    <span>{item.title}</span>
                  </span>
                  {item.details?.map((detail) => <span key={detail} className={dashboardStyles.meta}>{detail}</span>)}
                  {item.warnings?.map((warning) => (
                    <span key={warning} className={dashboardStyles.error}>{warning}</span>
                  ))}
                  {error && <span className={dashboardStyles.error}>{error}</span>}
                </div>
                {item.actions.length > 0 && (
                  <div className={dashboardStyles.actions}>
                    {item.actions.map((action) => (
                      <button key={action.label} type='button' disabled={busy}
                              className={action.secondary ? dashboardStyles.buttonSecondary : dashboardStyles.button}
                              onClick={() => run(item.key, action.url, action.body, action.method)}>
                        {action.label}
                      </button>
                    ))}
                  </div>
                )}
                {item.vote && <TimeChangeVote proposal={item.vote.proposal} timeZone={item.vote.timeZone}/>}
                {item.invite && (
                  <InviteResponseActions inviteId={item.invite.inviteId} conflicts={item.invite.conflicts}
                                         timeZone={item.invite.timeZone}/>
                )}
              </li>
            );
          })}
        </ul>
      )}
      {truncated && <p className={dashboardStyles.meta}>Showing the most recent ones. Answer some to see the rest.</p>}
    </section>
  );
}
