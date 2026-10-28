import EmailForm from '@/app/components/dashboard/settings/email-form';
import PasswordForm from '@/app/components/dashboard/settings/password-form';
import SignOutEverywhere from '@/app/components/dashboard/settings/sign-out-everywhere';
import DeleteAccount from '@/app/components/dashboard/settings/delete-account';
import {loadProfile} from '../load-profile';

export default async function AccountSettingsPage() {
  const result = await loadProfile();
  if ('failed' in result) {
    return result.failed;
  }
  const {profile} = result;

  return (
    <>
      <EmailForm profile={profile}/>
      <PasswordForm username={profile.username}/>
      <SignOutEverywhere/>
      <DeleteAccount username={profile.username}/>
    </>
  );
}
