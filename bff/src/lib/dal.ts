import { cache } from 'react';
import { redirect } from 'next/navigation';
import { getSession, type SessionData } from './session';

export const verifySession = cache(async (): Promise<SessionData> => {
  const session = await getSession();
  if (!session) {
    redirect('/login');
  }
  return session;
});