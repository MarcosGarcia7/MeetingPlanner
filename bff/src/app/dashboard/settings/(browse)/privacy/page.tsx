import PrivacyToggles from '@/app/components/dashboard/settings/privacy-toggles';
import {loadProfile} from '../load-profile';

export default async function PrivacySettingsPage() {
  const result = await loadProfile();
  if ('failed' in result) {
    return result.failed;
  }
  return <PrivacyToggles profile={result.profile}/>;
}
