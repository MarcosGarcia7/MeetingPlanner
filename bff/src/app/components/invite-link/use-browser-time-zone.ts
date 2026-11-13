'use client';

import {useSyncExternalStore} from 'react';

const subscribeNoop = () => () => {};

// The viewer's browser time zone, or null during server render. Link recipients often have no
// profile time zone, and the server renders in its own (UTC), so times are filled in on the client.
export function useBrowserTimeZone(): string | null {
  return useSyncExternalStore(
    subscribeNoop,
    () => Intl.DateTimeFormat().resolvedOptions().timeZone,
    () => null
  );
}
