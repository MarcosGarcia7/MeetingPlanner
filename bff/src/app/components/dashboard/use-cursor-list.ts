'use client';

import {useState} from 'react';
import {useRouter} from 'next/navigation';
import type {CursorPage} from '@/lib/types/api';
import {sendJson} from '@/lib/send-json';

// A list that starts from a server-rendered first page and fetches further pages from a BFF route.
// `url` is the list route without a cursor; `initial` is null when the server-side request failed.
// `setItems` lets callers add or remove items after their own actions.
export function useCursorList<T>(url: string, initial: CursorPage<T> | null, failedMessage: string) {
  const router = useRouter();
  const [items, setItems] = useState<T[]>(initial?.items ?? []);
  const [cursor, setCursor] = useState(initial?.nextCursor ?? null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(initial === null ? failedMessage : null);

  async function loadMore() {
    if (!cursor) {
      return;
    }
    setLoading(true);
    setError(null);
    const separator = url.includes('?') ? '&' : '?';
    const result = await sendJson<CursorPage<T>>(`${url}${separator}cursor=${encodeURIComponent(cursor)}`);
    setLoading(false);
    if (!result.ok) {
      if (result.unauthenticated) {
        router.push('/login');
        return;
      }
      setError(result.message);
      return;
    }
    setItems((current) => [...current, ...result.data.items]);
    setCursor(result.data.nextCursor);
  }

  return {items, setItems, loadMore, loading, error, hasMore: cursor !== null, loaded: initial !== null};
}
