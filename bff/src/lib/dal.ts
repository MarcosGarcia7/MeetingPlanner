import 'server-only';
import {cache} from 'react';
import {redirect} from 'next/navigation';
import {getSession} from './session';

export interface SessionUser {
  userId: number;
  username: string;
}

// The signed-in user, or null for pages anyone can open (e.g. invite links). Like verifySession,
// it returns only the user's identity.
export const getSessionUser = cache(async (): Promise<SessionUser | null> => {
  const session = await getSession();
  return session ? {userId: session.userId, username: session.username} : null;
});

// Returns only the user's identity so tokens can't be passed to client components by accident.
export const verifySession = cache(async (): Promise<SessionUser> => {
  const user = await getSessionUser();
  if (!user) {
    redirect('/login');
  }
  return user;
});
