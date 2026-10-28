import {headers} from 'next/headers';
import {redirect} from 'next/navigation';
import {verifySession} from '@/lib/dal';
import {AuthError, authFetch, getClientIp} from '@/lib/api-client';
import type {UserProfileResponse} from '@/lib/types/user';
import dashboardStyles from '@/app/components/dashboard/dashboard.module.css';

// Shared by the Privacy and Account tabs: the profile, or the error card to render in its place.
export async function loadProfile(): Promise<{ profile: UserProfileResponse } | { failed: React.ReactNode }> {
  await verifySession();
  try {
    return {profile: await authFetch<UserProfileResponse>('/api/users/me', {}, getClientIp(await headers()))};
  } catch (err) {
    if (err instanceof AuthError) {
      redirect('/login');
    }
    return {
      failed: (
        <section className={dashboardStyles.card}>
          <p className={dashboardStyles.error} role='alert'>Couldn’t load your account. Please refresh to try again.</p>
        </section>
      ),
    };
  }
}
