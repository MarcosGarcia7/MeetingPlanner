import {headers} from 'next/headers';
import {redirect} from 'next/navigation';
import {verifySession} from '@/lib/dal';
import {AuthError, authFetch, getClientIp} from '@/lib/api-client';
import type {UserProfileResponse} from '@/lib/types/user';
import ProfileHeader from '@/app/components/dashboard/profile/profile-header';
import ProfileForm from '@/app/components/dashboard/profile/profile-form';
import PrivacyToggles from '@/app/components/dashboard/profile/privacy-toggles';
import EmailForm from '@/app/components/dashboard/profile/email-form';
import PasswordForm from '@/app/components/dashboard/profile/password-form';
import dashboardStyles from '@/app/components/dashboard/dashboard.module.css';
import styles from '@/app/components/dashboard/profile/profile.module.css';

export default async function ProfilePage() {
  await verifySession();
  const clientIp = getClientIp(await headers());

  let profile: UserProfileResponse;
  try {
    profile = await authFetch<UserProfileResponse>('/api/users/me', {}, clientIp);
  } catch (err) {
    if (err instanceof AuthError) {
      redirect('/login');
    }
    return (
      <main className={styles.page}>
        <section className={dashboardStyles.card}>
          <p className={dashboardStyles.error} role='alert'>Couldn’t load your profile. Please refresh to try again.</p>
        </section>
      </main>
    );
  }

  return (
    <main className={styles.page}>
      <ProfileHeader profile={profile}/>
      <ProfileForm profile={profile}/>
      <PrivacyToggles profile={profile}/>
      <EmailForm profile={profile}/>
      <PasswordForm username={profile.username}/>
    </main>
  );
}
