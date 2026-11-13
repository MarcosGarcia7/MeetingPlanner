import type {FriendshipResponse} from '@/lib/types/friend';
import type {InviteResponse} from '@/lib/types/invite';
import type {TimeChangeProposalResponse} from '@/lib/types/meeting';
import type {JoinRequestResponse} from '@/lib/types/request';
import {formatMeetingTime} from '@/app/components/dashboard/home/format';
import type {InboxItem} from './action-list';

// Builders that turn API rows into ActionList items. Used by the Inbox pages on the server.

const kindLabel = (kind: string | null) => (kind === 'GROUP' ? 'Group' : 'Meeting');
const kindPath = (kind: string | null) => (kind === 'GROUP' ? 'group' : 'meeting');

function friendName(friend: FriendshipResponse) {
  const fullName = [friend.firstName, friend.lastName].filter(Boolean).join(' ');
  return fullName ? `${fullName} (@${friend.username})` : `@${friend.username}`;
}

// A meeting invite's time ("Tue, Oct 27 · 2:00 – 3:30 PM") and location, in the profile time zone.
// Shared with the dashboard's "Needs your attention" card.
export function inviteDetails(invite: InviteResponse, timeZone: string): string[] {
  if (invite.kind !== 'MEETING') return [];
  const details: string[] = [];
  if (invite.meetingStartTime) {
    const {day, time} = formatMeetingTime(invite.meetingStartTime, invite.meetingEndTime, timeZone);
    details.push(`${day} · ${time}`);
  }
  const location = invite.meetingLocation?.trim();
  if (location) details.push(location);
  return details;
}

export function inviteItem(invite: InviteResponse, timeZone: string): InboxItem {
  const base = `/api/invites/${kindPath(invite.kind)}/${invite.id}`;
  const target = (invite.kind === 'GROUP' ? invite.groupName : invite.meetingTitle) ?? `a ${kindPath(invite.kind)}`;
  const item: InboxItem = {
    key: `invite-${invite.kind}-${invite.id}`,
    kind: kindLabel(invite.kind),
    title: <>@{invite.senderUsername} invited you to <strong>{target}</strong></>,
    details: inviteDetails(invite, timeZone),
    actions: [],
  };
  // Meeting invites can clash with the viewer's schedule, so InviteResponseActions answers them; group
  // invites keep plain Accept/Decline.
  return invite.kind === 'MEETING'
    ? {...item, invite: {inviteId: invite.id, conflicts: invite.conflicts, timeZone}}
    : {...item, actions: [
      {label: 'Accept', url: `${base}/accept`},
      {label: 'Decline', url: `${base}/decline`, secondary: true},
    ]};
}

export function incomingRequestItem(request: JoinRequestResponse): InboxItem {
  const base = `/api/requests/${kindPath(request.kind)}/${request.id}`;
  const target = (request.kind === 'GROUP' ? request.groupName : request.meetingTitle) ?? `your ${kindPath(request.kind)}`;
  return {
    key: `incoming-${request.kind}-${request.id}`,
    kind: kindLabel(request.kind),
    title: <>@{request.requesterUsername} wants to join <strong>{target}</strong></>,
    actions: [
      {label: 'Approve', url: `${base}/approve`},
      {label: 'Decline', url: `${base}/decline`, secondary: true},
    ],
  };
}

export function sentRequestItem(request: JoinRequestResponse): InboxItem {
  const target = (request.kind === 'GROUP' ? request.groupName : request.meetingTitle) ?? `a ${kindPath(request.kind)}`;
  return {
    key: `sent-${request.kind}-${request.id}`,
    kind: kindLabel(request.kind),
    title: <>You asked to join <strong>{target}</strong></>,
    details: ['Waiting for an organizer to respond'],
    actions: [
      {label: 'Cancel request', url: `/api/requests/${kindPath(request.kind)}/${request.id}/cancel`, secondary: true},
    ],
  };
}

export function incomingFriendItem(friend: FriendshipResponse): InboxItem {
  const base = `/api/users/me/friends/requests/${encodeURIComponent(friend.username)}`;
  return {
    key: `friend-in-${friend.userId}`,
    kind: 'Friend',
    title: <><strong>{friendName(friend)}</strong> wants to be friends</>,
    actions: [
      {label: 'Accept', url: `${base}/accept`},
      {label: 'Decline', url: `${base}/decline`, secondary: true},
    ],
  };
}

export function outgoingFriendItem(friend: FriendshipResponse): InboxItem {
  return {
    key: `friend-out-${friend.userId}`,
    kind: 'Friend',
    title: <>You sent <strong>{friendName(friend)}</strong> a friend request</>,
    actions: [{
      label: 'Cancel request',
      url: `/api/users/me/friends/requests/${encodeURIComponent(friend.username)}`,
      method: 'DELETE',
      secondary: true,
    }],
  };
}

export function voteItem(proposal: TimeChangeProposalResponse, timeZone: string): InboxItem {
  const {day, time} = formatMeetingTime(proposal.proposedStartTime, proposal.proposedEndTime, timeZone);
  return {
    key: `vote-${proposal.id}`,
    kind: 'Meeting',
    title: <strong>{proposal.meetingTitle ?? 'Meeting'}</strong>,
    details: [`@${proposal.proposedByUsername} proposed ${day}, ${time}`],
    // Approve/Decline, and the ways out of a clash, come from TimeChangeVote.
    actions: [],
    vote: {proposal, timeZone},
  };
}

export function ownProposalItem(proposal: TimeChangeProposalResponse, timeZone: string): InboxItem {
  const {day, time} = formatMeetingTime(proposal.proposedStartTime, proposal.proposedEndTime, timeZone);
  return {
    key: `proposal-${proposal.id}`,
    kind: 'Meeting',
    title: <strong>{proposal.meetingTitle ?? 'Meeting'}</strong>,
    details: [`You proposed ${day}, ${time}`, 'Waiting for every participant to vote'],
    actions: [{
      label: 'Cancel proposal',
      url: `/api/meetings/${proposal.meetingId}/time-change/${proposal.id}/cancel`,
      secondary: true,
    }],
  };
}
