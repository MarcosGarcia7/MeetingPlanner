'use client';

import {useState} from 'react';
import {useForm} from 'react-hook-form';
import {useRouter} from 'next/navigation';
import {zodResolver} from '@hookform/resolvers/zod';
import {z} from 'zod';
import type {GroupResponse, GroupUpdateRequest} from '@/lib/types/group';
import {sendJson} from '@/lib/send-json';
import type {GroupRole} from './load-group';
import authStyles from '@/app/components/auth/auth.module.css';
import dashboardStyles from '@/app/components/dashboard/dashboard.module.css';
import styles from './groups.module.css';

const settingsSchema = z.object({
  groupName: z.string().trim().min(1, 'Give your group a name').max(50, 'Group name cannot exceed 50 characters'),
  isPrivate: z.boolean(),
  membersCanPost: z.boolean(),
  commentsEnabled: z.boolean(),
  membersCanCreate: z.boolean(),
});
type SettingsFormData = z.infer<typeof settingsSchema>;

function toFormData(group: GroupResponse): SettingsFormData {
  return {
    groupName: group.groupName ?? '',
    isPrivate: group.isPrivate,
    membersCanPost: group.membersCanPost,
    commentsEnabled: group.commentsEnabled,
    membersCanCreate: group.membersCanCreate,
  };
}

// Group settings. Admins can change who may post and create meetings; the name and privacy are the
// owner's alone, so admins see those fields disabled.
export default function GroupSettingsForm({group, role}: { group: GroupResponse; role: GroupRole }) {
  const router = useRouter();
  const [status, setStatus] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const isOwner = role === 'OWNER';
  const {register, handleSubmit, reset, formState: {errors, isSubmitting, isDirty, dirtyFields}} =
    useForm<SettingsFormData>({resolver: zodResolver(settingsSchema), defaultValues: toFormData(group)});

  const onSubmit = async (data: SettingsFormData) => {
    setStatus(null);
    setError(null);
    // PATCH semantics: send only what changed.
    const body: GroupUpdateRequest = {
      groupName: dirtyFields.groupName ? data.groupName : undefined,
      isPrivate: dirtyFields.isPrivate ? data.isPrivate : undefined,
      membersCanPost: dirtyFields.membersCanPost ? data.membersCanPost : undefined,
      commentsEnabled: dirtyFields.commentsEnabled ? data.commentsEnabled : undefined,
      membersCanCreate: dirtyFields.membersCanCreate ? data.membersCanCreate : undefined,
    };
    const result = await sendJson<GroupResponse>(`/api/groups/${group.id}`, 'PATCH', body);
    if (!result.ok) {
      if (result.unauthenticated) {
        router.push('/login');
        return;
      }
      setError(result.message);
      return;
    }
    reset(toFormData(result.data));
    setStatus('Settings saved.');
    // The header and tabs show the name and privacy too.
    router.refresh();
  };

  return (
    <section className={dashboardStyles.card} aria-labelledby='group-settings-title'>
      <div className={dashboardStyles.cardHeader}>
        <h2 id='group-settings-title' className={dashboardStyles.cardTitle}>Settings</h2>
      </div>
      <form onSubmit={handleSubmit(onSubmit)} className={authStyles.form} noValidate>
        <label className={authStyles.field}>
          <span className={authStyles.label}>Group name</span>
          <input type='text' maxLength={50} disabled={!isOwner} {...register('groupName')}
                 className={authStyles.input}/>
          {errors.groupName && <span className={authStyles.error} role='alert'>{errors.groupName.message}</span>}
        </label>
        <label className={styles.checkboxField}>
          <input type='checkbox' disabled={!isOwner} {...register('isPrivate')}/>
          Private: people must request to join or be invited
        </label>
        {!isOwner && <p className={dashboardStyles.meta}>Only the owner can rename the group or change its privacy.</p>}
        <fieldset className={styles.fieldset}>
          <legend className={authStyles.label}>Members can</legend>
          <label className={styles.checkboxField}>
            <input type='checkbox' {...register('membersCanPost')}/>
            Write posts
          </label>
          <label className={styles.checkboxField}>
            <input type='checkbox' {...register('membersCanCreate')}/>
            Schedule meetings
          </label>
        </fieldset>
        <label className={styles.checkboxField}>
          <input type='checkbox' {...register('commentsEnabled')}/>
          Allow comments on posts (the owner can still turn them off or on per post)
        </label>
        {error && <p className={authStyles.error} role='alert'>{error}</p>}
        <div className={styles.formFooter}>
          {status && !isDirty && <span className={styles.status} role='status'>{status}</span>}
          <button type='button' className={dashboardStyles.buttonSecondary} disabled={!isDirty || isSubmitting}
                  onClick={() => {
                    reset();
                    setError(null);
                  }}>
            Cancel
          </button>
          <button type='submit' className={dashboardStyles.button} disabled={!isDirty || isSubmitting}>
            {isSubmitting ? 'Saving…' : 'Save changes'}
          </button>
        </div>
      </form>
    </section>
  );
}
