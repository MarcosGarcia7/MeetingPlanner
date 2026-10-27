'use client';

import {useState} from 'react';
import {useForm} from 'react-hook-form';
import {useRouter} from 'next/navigation';
import {zodResolver} from '@hookform/resolvers/zod';
import {z} from 'zod';
import {readApiError} from '@/lib/api-error';
import type {GroupCreateRequest, GroupResponse} from '@/lib/types/group';
import authStyles from '@/app/components/auth/auth.module.css';
import dashboardStyles from '@/app/components/dashboard/dashboard.module.css';
import styles from './groups.module.css';

const createGroupSchema = z.object({
  groupName: z.string().trim().min(1, 'Give your group a name').max(50, 'Group name cannot exceed 50 characters'),
  isPrivate: z.boolean(),
});
type CreateGroupFormData = z.infer<typeof createGroupSchema>;

// Creates a group and opens its page.
export default function CreateGroupForm({onCancel}: { onCancel: () => void }) {
  const router = useRouter();
  const [error, setError] = useState<string | null>(null);
  const {register, handleSubmit, formState: {errors, isSubmitting}} = useForm<CreateGroupFormData>({
    resolver: zodResolver(createGroupSchema),
    defaultValues: {groupName: '', isPrivate: false},
  });

  const onSubmit = async (data: CreateGroupFormData) => {
    setError(null);
    const body: GroupCreateRequest = {groupName: data.groupName, isPrivate: data.isPrivate};
    try {
      const response = await fetch('/api/groups', {
        method: 'POST',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify(body),
      });
      if (!response.ok) {
        const apiError = await readApiError(response);
        if (apiError?.error === 'unauthenticated') {
          router.push('/login');
          return;
        }
        setError(apiError?.message ?? 'Could not create the group. Please try again.');
        return;
      }
      const group = (await response.json()) as GroupResponse;
      router.push(`/dashboard/groups/${group.id}`);
    } catch {
      setError('Couldn’t reach the server. Please try again.');
    }
  };

  return (
    <form onSubmit={handleSubmit(onSubmit)} className={authStyles.form} noValidate>
      <label className={authStyles.field}>
        <span className={authStyles.label}>Group name</span>
        <input type='text' maxLength={50} autoFocus {...register('groupName')} className={authStyles.input}/>
        {errors.groupName && <span className={authStyles.error} role='alert'>{errors.groupName.message}</span>}
      </label>
      <label className={styles.checkboxField}>
        <input type='checkbox' {...register('isPrivate')}/>
        Private: people must request to join
      </label>
      {error && <p className={authStyles.error} role='alert'>{error}</p>}
      <div className={styles.formFooter}>
        <button type='button' className={dashboardStyles.buttonSecondary} onClick={onCancel} disabled={isSubmitting}>
          Cancel
        </button>
        <button type='submit' className={dashboardStyles.button} disabled={isSubmitting}>
          {isSubmitting ? 'Creating…' : 'Create group'}
        </button>
      </div>
    </form>
  );
}
