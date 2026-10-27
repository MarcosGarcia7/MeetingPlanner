'use client';

import {useState, useTransition} from 'react';
import {useRouter} from 'next/navigation';
import {readApiError} from '@/lib/api-error';

interface ActionError {
  key: string;
  message: string;
}

// Calls a BFF route for one list item (POST unless `method` says otherwise), then re-renders the server
// page so every card reflects the change. `key` identifies the item so its error shows next to it.
// Resolves to whether it succeeded.
export function usePostAction() {
  const router = useRouter();
  const [pendingKey, setPendingKey] = useState<string | null>(null);
  const [error, setError] = useState<ActionError | null>(null);
  const [isRefreshing, startTransition] = useTransition();

  async function run(key: string, url: string, body?: object, method = 'POST'): Promise<boolean> {
    setPendingKey(key);
    setError(null);
    try {
      const response = await fetch(url, {
        method,
        headers: body ? {'Content-Type': 'application/json'} : undefined,
        body: body ? JSON.stringify(body) : undefined,
      });
      if (!response.ok) {
        const apiError = await readApiError(response);
        if (apiError?.error === 'unauthenticated') {
          router.push('/login');
          return false;
        }
        setError({key, message: apiError?.message ?? 'Something went wrong. Please try again.'});
        return false;
      }
      startTransition(() => router.refresh());
      return true;
    } catch {
      setError({key, message: 'Couldn’t reach the server. Please try again.'});
      return false;
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
