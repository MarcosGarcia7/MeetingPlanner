'use client';

import {useRef, useState} from 'react';
import {useRouter} from 'next/navigation';
import clsx from 'clsx';
import type {CursorPage} from '@/lib/types/api';
import {sendJson} from '@/lib/send-json';
import dashboardStyles from '@/app/components/dashboard/dashboard.module.css';
import homeStyles from '@/app/components/dashboard/home/home.module.css';
import styles from './inbox.module.css';

export interface HistoryRow {
  key: string;
  kind: string;
  // "Accepted", "Declined", …
  status: string;
  title: React.ReactNode;
  meta?: string;
}

export interface HistoryView {
  label: string;
  // BFF list route (all statuses); `cursor=` is appended for later pages.
  url: string;
}

interface HistoryCardProps<T> {
  views: HistoryView[];
  // Null skips the item: the list endpoints return every status, and pending items are shown above.
  toRow: (item: T) => HistoryRow | null;
  empty: string;
}

interface ListState<T> {
  items: T[];
  cursor: string | null;
  loading: boolean;
  error: string | null;
}

// Collapsed "Past" card under a tab's pending lists. Nothing is fetched until it is first opened.
export default function HistoryCard<T>({views, toRow, empty}: HistoryCardProps<T>) {
  const router = useRouter();
  const [open, setOpen] = useState(false);
  const [view, setView] = useState(0);
  const [list, setList] = useState<ListState<T> | null>(null);
  // Ignores a page that arrives after the view was switched.
  const requestId = useRef(0);

  async function fetchPage(viewIndex: number, cursor: string | null) {
    const id = ++requestId.current;
    setList((current) => ({
      items: cursor && current ? current.items : [],
      cursor,
      loading: true,
      error: null,
    }));
    const url = views[viewIndex].url;
    const separator = url.includes('?') ? '&' : '?';
    const result = await sendJson<CursorPage<T>>(cursor ? `${url}${separator}cursor=${encodeURIComponent(cursor)}` : url);
    if (id !== requestId.current) {
      return;
    }
    if (!result.ok) {
      if (result.unauthenticated) {
        router.push('/login');
        return;
      }
      setList((current) => current && {...current, loading: false, error: result.message});
      return;
    }
    setList((current) => ({
      items: [...(cursor && current ? current.items : []), ...result.data.items],
      cursor: result.data.nextCursor,
      loading: false,
      error: null,
    }));
  }

  function onToggle(event: React.SyntheticEvent<HTMLDetailsElement>) {
    const isOpen = event.currentTarget.open;
    setOpen(isOpen);
    if (isOpen && list === null) {
      void fetchPage(view, null);
    }
  }

  function selectView(index: number) {
    setView(index);
    void fetchPage(index, null);
  }

  const rows = list?.items.flatMap((item) => toRow(item) ?? []) ?? [];

  return (
    <section className={dashboardStyles.card}>
      <details className={styles.history} onToggle={onToggle}>
        <summary>Past</summary>
        {open && (
          <div className={styles.historyBody}>
            {views.length > 1 && (
              <div className={dashboardStyles.filters} role='group' aria-label='Which history'>
                {views.map((v, index) => (
                  <button key={v.url} type='button' aria-pressed={index === view}
                          className={clsx(dashboardStyles.filter, {[dashboardStyles.currentFilter]: index === view})}
                          onClick={() => selectView(index)}>
                    {v.label}
                  </button>
                ))}
              </div>
            )}
            {rows.length > 0 ? (
              <ul className={homeStyles.cardList}>
                {rows.map((row) => (
                  <li key={row.key} className={dashboardStyles.itemRow}>
                    <div className={homeStyles.itemBody}>
                      <span className={clsx(homeStyles.itemTitle, styles.rowTitle)}>
                        <span className={styles.chip}>{row.kind}</span>
                        <span>{row.title}</span>
                      </span>
                      {row.meta && <span className={dashboardStyles.meta}>{row.meta}</span>}
                    </div>
                    <span className={styles.statusChip}>{row.status}</span>
                  </li>
                ))}
              </ul>
            ) : (
              list && !list.loading && !list.error && !list.cursor && <p className={dashboardStyles.empty}>{empty}</p>
            )}
            {list?.error && <p className={dashboardStyles.error}>{list.error}</p>}
            {list?.loading && <p className={dashboardStyles.meta}>Loading…</p>}
            {list && !list.loading && list.cursor && (
              <div className={styles.loadMore}>
                <button type='button' className={dashboardStyles.buttonSecondary}
                        onClick={() => fetchPage(view, list.cursor)}>
                  Load more
                </button>
              </div>
            )}
          </div>
        )}
      </details>
    </section>
  );
}
