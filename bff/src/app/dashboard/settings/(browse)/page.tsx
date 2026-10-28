import {headers} from 'next/headers';
import {redirect} from 'next/navigation';
import {verifySession} from '@/lib/dal';
import {AuthError, authFetch, getClientIp} from '@/lib/api-client';
import type {NotificationPreferencesResponse} from '@/lib/types/notification';
import NotificationPreferences from '@/app/components/dashboard/settings/notification-preferences';
import dashboardStyles from '@/app/components/dashboard/dashboard.module.css';

export default async function NotificationSettingsPage() {
  await verifySession();
  const clientIp = getClientIp(await headers());

  let preferences: NotificationPreferencesResponse;
  try {
    preferences = await authFetch<NotificationPreferencesResponse>(
      '/api/users/me/notification-preferences', {}, clientIp
    );
  } catch (err) {
    if (err instanceof AuthError) {
      redirect('/login');
    }
    return (
      <section className={dashboardStyles.card}>
        <p className={dashboardStyles.error} role='alert'>
          Couldn’t load your notification settings. Please refresh to try again.
        </p>
      </section>
    );
  }

  return <NotificationPreferences preferences={preferences}/>;
}
