import 'server-only';
import { cache } from 'react';
import { redirect } from 'next/navigation';
import { getSession } from './session';

export interface SessionUser {
  userId: number;
  username: string;
}

// Returns only the user's identity so tokens can't be passed to client components by accident.
export const verifySession = cache(async (): Promise<SessionUser> => {
  const session = await getSession();
  if (!session) {
    redirect('/login');
  }
  return { userId: session.userId, username: session.username };
});
