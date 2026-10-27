import type {NotificationResponse} from '@/lib/types/notification';

export interface NotificationView {
  text: string;
  // Dashboard page the notification points at, if any.
  href: string | null;
}

// Server links are app URLs (`…/groups/5/posts/9`, `…/meetings/7`, `…/friends/requests`); map them to dashboard pages.
function dashboardHref(link: string | undefined): string | null {
  if (!link) {
    return null;
  }
  let path: string;
  try {
    path = new URL(link).pathname;
  } catch {
    return null;
  }
  const group = path.match(/^\/groups\/(\d+)/);
  if (group) {
    return `/dashboard/groups/${group[1]}`;
  }
  const meeting = path.match(/^\/meetings\/(\d+)/);
  if (meeting) {
    return `/dashboard/schedule/${meeting[1]}`;
  }
  if (path.startsWith('/friends')) {
    return '/dashboard/inbox/requests';
  }
  return null;
}

function parsePayload(payload: string | null): Record<string, string> {
  if (!payload) {
    return {};
  }
  try {
    const parsed: unknown = JSON.parse(payload);
    return parsed && typeof parsed === 'object' ? parsed as Record<string, string> : {};
  } catch {
    return {};
  }
}

// One-line text for a notification, built from its type and payload variables.
export function describeNotification(notification: NotificationResponse): NotificationView {
  const v = parsePayload(notification.payload);
  const href = dashboardHref(v.link);
  const meeting = `“${v.title ?? 'a meeting'}”`;
  const group = v.group ?? 'a group';

  switch (notification.type) {
    case 'FRIEND_REQUEST':
      return {text: `${v.requester ?? 'Someone'} sent you a friend request`, href: '/dashboard/inbox/requests'};
    case 'FRIEND_REQUEST_ACCEPTED':
      return {text: `${v.acceptor ?? 'Someone'} accepted your friend request`, href: '/dashboard/friends'};
    case 'GROUP_INVITE':
      return {text: `@${v.sender} invited you to join ${group}`, href: '/dashboard/inbox/invites'};
    case 'MEETING_INVITE':
      return {text: `@${v.sender} invited you to ${meeting}`, href: '/dashboard/inbox/invites'};
    case 'GROUP_JOIN_REQUEST':
      return {text: `@${v.requester} asked to join ${group}`, href: '/dashboard/inbox/requests'};
    case 'MEETING_JOIN_REQUEST':
      return {text: `@${v.requester} asked to join ${meeting}`, href: '/dashboard/inbox/requests'};
    case 'GROUP_JOIN_REQUEST_APPROVED':
      return {text: `Your request to join ${group} was approved`, href};
    case 'GROUP_JOIN_REQUEST_DECLINED':
      return {text: `Your request to join ${group} was declined`, href: null};
    case 'MEETING_JOIN_REQUEST_APPROVED':
      return {text: `Your request to join ${meeting} was approved`, href};
    case 'MEETING_JOIN_REQUEST_DECLINED':
      return {text: `Your request to join ${meeting} was declined`, href: null};
    case 'GROUP_POST':
      return {text: `${v.author ?? 'Someone'} posted in ${group}: “${v.body ?? ''}”`, href};
    case 'POST_COMMENT':
      return {text: `${v.author ?? 'Someone'} commented on your post: “${v.body ?? ''}”`, href};
    case 'MEETING_REMINDER':
      return {text: `${meeting} starts soon`, href};
    case 'MEETING_UPDATED':
      return {text: `${meeting} was updated`, href};
    case 'MEETING_DELETED':
      return {text: `${meeting} was deleted`, href: null};
    case 'MEETING_TIME_CHANGE_REQUEST':
      return {text: `A new time was proposed for ${meeting}`, href: '/dashboard/inbox/proposals'};
    case 'MEETING_TIME_CHANGE_APPROVED':
      return {text: `Everyone approved the new time for ${meeting}`, href};
    case 'MEETING_TIME_CHANGE_REJECTED':
      return {text: `The time change for ${meeting} was rejected`, href};
    case 'MEETING_TIME_CHANGE_EXPIRED':
      return {text: `The time change for ${meeting} expired without enough votes`, href};
    case 'MEETING_TIME_CHANGE_CANCELLED':
      return {text: `The time change for ${meeting} was cancelled`, href};
    default:
      return {text: (notification.type ?? 'Notification').toLowerCase().replaceAll('_', ' '), href};
  }
}
