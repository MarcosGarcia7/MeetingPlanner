'use client';

import {useEffect, useState} from 'react';
import {useRouter} from 'next/navigation';
import type {CursorPage} from '@/lib/types/api';
import type {GroupSummaryResponse} from '@/lib/types/group';
import {readApiError} from '@/lib/api-error';
import GroupCard from './group-card';
import authStyles from '@/app/components/auth/auth.module.css';
import dashboardStyles from '@/app/components/dashboard/dashboard.module.css';
import styles from './groups.module.css';

const DEBOUNCE_MS = 300;

type SearchState =
  | {status: 'idle'}
  | {status: 'loading'}
  | {status: 'error'; message: string}
  | {status: 'done'; groups: GroupSummaryResponse[]};

export default function GroupSearch() {
  const router = useRouter();
  const [query, setQuery] = useState('');
  const [state, setState] = useState<SearchState>({status: 'idle'});
  // Bumped after a join/request from a result so the list re-fetches with the new viewer status.
  const [version, setVersion] = useState(0);
  const trimmed = query.trim();

  useEffect(() => {
    if (!trimmed) {
      return;
    }
    const controller = new AbortController();
    const timer = setTimeout(async () => {
      setState({status: 'loading'});
      try {
        const response = await fetch(`/api/search/groups?q=${encodeURIComponent(trimmed)}`,
          {signal: controller.signal});
        if (!response.ok) {
          const apiError = await readApiError(response);
          if (apiError?.error === 'unauthenticated') {
            router.push('/login');
            return;
          }
          setState({status: 'error', message: apiError?.message ?? 'Search failed. Please try again.'});
          return;
        }
        const page = (await response.json()) as CursorPage<GroupSummaryResponse>;
        setState({status: 'done', groups: page.items});
      } catch {
        if (!controller.signal.aborted) {
          setState({status: 'error', message: 'Couldn’t reach the server. Please try again.'});
        }
      }
    }, DEBOUNCE_MS);
    return () => {
      clearTimeout(timer);
      controller.abort();
    };
  }, [trimmed, version, router]);

  return (
    <section className={dashboardStyles.card}>
      <div className={dashboardStyles.cardHeader}>
        <h2 className={dashboardStyles.cardTitle}>Find a group</h2>
      </div>
      <input type='search' className={authStyles.input} placeholder='Search groups by name'
             aria-label='Search groups by name' value={query} maxLength={100}
             onChange={(event) => setQuery(event.target.value)}/>
      {trimmed && (
        <div className={styles.searchResults}>
          {state.status === 'loading' && <p className={dashboardStyles.empty}>Searching…</p>}
          {state.status === 'error' && <p className={dashboardStyles.error}>{state.message}</p>}
          {state.status === 'done' && (state.groups.length === 0 ? (
            <p className={dashboardStyles.empty}>No groups match “{trimmed}”.</p>
          ) : (
            <ul className={styles.groupList}>
              {state.groups.map((group) => (
                <GroupCard key={group.id} group={group} onChanged={() => setVersion((v) => v + 1)}/>
              ))}
            </ul>
          ))}
        </div>
      )}
    </section>
  );
}
