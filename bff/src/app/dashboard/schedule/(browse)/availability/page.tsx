import {headers} from 'next/headers';
import {redirect} from 'next/navigation';
import {verifySession} from '@/lib/dal';
import {ApiError, authFetch, getClientIp} from '@/lib/api-client';
import {hasAuthError, valueOrNull} from '@/lib/settled';
import type {UserAvailabilityResponse, UserProfileResponse} from '@/lib/types/user';
import AvailabilityForm from '@/app/components/dashboard/schedule/availability-form';
import dashboardStyles from '@/app/components/dashboard/dashboard.module.css';
import {profileTimeZone} from '@/lib/timezone';

export default async function AvailabilityPage() {
  await verifySession();
  const clientIp = getClientIp(await headers());
  const get = <T, >(path: string) => authFetch<T>(path, {}, clientIp);

  const results = await Promise.allSettled([
    get<UserAvailabilityResponse>('/api/users/me/availability'),
    get<UserProfileResponse>('/api/users/me'),
  ] as const);

  if (hasAuthError(results)) {
    redirect('/login');
  }

  // 404: availability was never saved, so the form starts empty.
  const neverSet = results[0].status === 'rejected' && results[0].reason instanceof ApiError
    && results[0].reason.status === 404;
  if (results[0].status === 'rejected' && !neverSet) {
    return (
      <section className={dashboardStyles.card}>
        <p className={dashboardStyles.error}>Couldn’t load your availability.</p>
      </section>
    );
  }

  return <AvailabilityForm availability={valueOrNull(results[0])} timeZone={profileTimeZone(valueOrNull(results[1]))}/>;
}
