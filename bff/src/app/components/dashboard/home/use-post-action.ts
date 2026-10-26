'use client';

import {useState, useTransition} from 'react';
import {useRouter} from 'next/navigation';
import {readApiError} from '@/lib/api-error';

interface ActionError {
  key: string;
  message: string;
}

// POSTs to a BFF route for one list item, then re-renders the server page so every card reflects
// the change. `key` identifies the item so its error shows next to it.
export function usePostAction() {
  const router = useRouter();
  const [pendingKey, setPendingKey] = useState<string | null>(null);
  const [error, setError] = useState<ActionError | null>(null);
  const [isRefreshing, startTransition] = useTransition();

  async function run(key: string, url: string, body?: object) {
    setPendingKey(key);
    setError(null);
    try {
      const response = await fetch(url, {
        method: 'POST',
        headers: body ? {'Content-Type': 'application/json'} : undefined,
        body: body ? JSON.stringify(body) : undefined,
      });
      if (!response.ok) {
        const apiError = await readApiError(response);
        if (apiError?.error === 'unauthenticated') {
          router.push('/login');
          return;
        }
        setError({key, message: apiError?.message ?? 'Something went wrong. Please try again.'});
        return;
      }
      startTransition(() => router.refresh());
    } catch {
      setError({key, message: 'Couldn’t reach the server. Please try again.'});
    } finally {
      setPendingKey(null);
    }
  }

  return {
    run,
    busy: pendingKey !== null || isRefreshing,
    errorFor: (key: string) => (error?.key === key ? error.message : null),
  };
}
