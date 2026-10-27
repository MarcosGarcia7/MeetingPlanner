import {AuthError} from '@/lib/api-client';

// Helpers for pages that load several sections with Promise.allSettled, so one failing call degrades
// its section instead of the page.

export function valueOrNull<T>(result: PromiseSettledResult<T>): T | null {
  return result.status === 'fulfilled' ? result.value : null;
}

// True when any call failed because the session is gone; the page should redirect to /login.
export function hasAuthError(results: readonly PromiseSettledResult<unknown>[]): boolean {
  return results.some((result) => result.status === 'rejected' && result.reason instanceof AuthError);
}
