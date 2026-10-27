'use client';

import {useEffect, useState} from 'react';
import {useRouter} from 'next/navigation';
import type {CursorPage} from '@/lib/types/api';
import type {UserSearchResult} from '@/lib/types/friend';
import {readApiError} from '@/lib/api-error';
import {usePostAction} from '@/app/components/dashboard/use-post-action';
import UserRow from './user-row';
import authStyles from '@/app/components/auth/auth.module.css';
import dashboardStyles from '@/app/components/dashboard/dashboard.module.css';
import styles from './friends.module.css';

const DEBOUNCE_MS = 300;

type SearchState =
  | {status: 'idle'}
  | {status: 'loading'}
  | {status: 'error'; message: string}
  | {status: 'done'; users: UserSearchResult[]};

// "Find people": debounced user search whose rows offer the next step for each relationship.
export default function UserSearch() {
  const router = useRouter();
  const [query, setQuery] = useState('');
  const [state, setState] = useState<SearchState>({status: 'idle'});
  // Bumped after an action on a result so the list re-fetches with the new relationship.
  const [version, setVersion] = useState(0);
  const {run, busy, errorFor} = usePostAction();
  const trimmed = query.trim();

  useEffect(() => {
    if (!trimmed) {
      return;
    }
    const controller = new AbortController();
    const timer = setTimeout(async () => {
      setState({status: 'loading'});
      try {
        const response = await fetch(`/api/search/users?q=${encodeURIComponent(trimmed)}`,
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
        const page = (await response.json()) as CursorPage<UserSearchResult>;
        setState({status: 'done', users: page.items});
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

  async function act(user: UserSearchResult, url: string, body?: object, method?: string) {
    if (await run(user.username, url, body, method)) {
      setVersion((v) => v + 1);
    }
  }

  function actionsFor(user: UserSearchResult) {
    const requestUrl = `/api/users/me/friends/requests/${encodeURIComponent(user.username)}`;
    switch (user.relationship) {
      case 'FRIENDS':
        return null;
      case 'REQUEST_SENT':
        return (
          <button type='button' className={dashboardStyles.buttonSecondary} disabled={busy}
                  onClick={() => act(user, requestUrl, undefined, 'DELETE')}>
            Cancel request
          </button>
        );
      case 'REQUEST_RECEIVED':
        return (
          <>
            <button type='button' className={dashboardStyles.button} disabled={busy}
                    onClick={() => act(user, `${requestUrl}/accept`)}>
              Accept
            </button>
            <button type='button' className={dashboardStyles.buttonSecondary} disabled={busy}
                    onClick={() => act(user, `${requestUrl}/decline`)}>
              Decline
            </button>
          </>
        );
      default:
        return (
          <button type='button' className={dashboardStyles.button} disabled={busy}
                  onClick={() => act(user, '/api/users/me/friends/requests', {username: user.username})}>
            Add friend
          </button>
        );
    }
  }

  const badges: Partial<Record<UserSearchResult['relationship'], string>> = {
    FRIENDS: 'Friends',
    REQUEST_SENT: 'Requested',
    REQUEST_RECEIVED: 'Wants to be friends',
  };

  return (
    <section className={dashboardStyles.card}>
      <div className={dashboardStyles.cardHeader}>
        <h2 className={dashboardStyles.cardTitle}>Find people</h2>
      </div>
      <input type='search' className={authStyles.input} placeholder='Search by name or username'
             aria-label='Search people by name or username' value={query} maxLength={100}
             onChange={(event) => setQuery(event.target.value)}/>
      {trimmed && (
        <div className={styles.searchResults}>
          {state.status === 'loading' && <p className={dashboardStyles.empty}>Searching…</p>}
          {state.status === 'error' && <p className={dashboardStyles.error}>{state.message}</p>}
          {state.status === 'done' && (state.users.length === 0 ? (
            <p className={dashboardStyles.empty}>No one matches “{trimmed}”.</p>
          ) : (
            <ul className={styles.list}>
              {state.users.map((user) => {
                const badge = badges[user.relationship];
                const actions = actionsFor(user);
                return (
                  <UserRow key={user.id} user={user} error={errorFor(user.username)}
                           badge={badge && <span className={styles.chip}>{badge}</span>}>
                    {actions && <div className={dashboardStyles.actions}>{actions}</div>}
                  </UserRow>
                );
              })}
            </ul>
          ))}
        </div>
      )}
    </section>
  );
}
