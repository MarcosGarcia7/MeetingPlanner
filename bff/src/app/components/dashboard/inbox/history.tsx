'use client';

import type {InviteResponse, InviteStatus} from '@/lib/types/invite';
import type {ProposalStatus, TimeChangeProposalResponse} from '@/lib/types/meeting';
import type {JoinRequestResponse, RequestStatus} from '@/lib/types/request';
import {formatDateTime, formatMeetingTime} from '@/app/components/dashboard/home/format';
import HistoryCard from './history-card';

// The "Past" card of each Inbox tab: answered invites, requests, and proposals.

const INVITE_STATUS: Record<InviteStatus, string> = {
  PENDING: 'Pending', ACCEPTED: 'Accepted', DECLINED: 'Declined', LEFT: 'Left',
};

const REQUEST_STATUS: Record<RequestStatus, string> = {
  PENDING: 'Pending', APPROVED: 'Approved', DECLINED: 'Declined', CANCELLED: 'Cancelled',
};

const PROPOSAL_STATUS: Record<ProposalStatus, string> = {
  PENDING: 'Pending', APPROVED: 'Approved', REJECTED: 'Rejected', EXPIRED: 'Expired', CANCELLED: 'Cancelled',
};

export function InviteHistory() {
  return (
    <HistoryCard<InviteResponse>
      views={[{label: 'All', url: '/api/invites'}]}
      empty='No answered invites yet.'
      toRow={(invite) => invite.status === 'PENDING' ? null : {
        key: `invite-${invite.kind}-${invite.id}`,
        kind: invite.kind === 'GROUP' ? 'Group' : 'Meeting',
        status: INVITE_STATUS[invite.status],
        title: (invite.kind === 'GROUP' ? invite.groupName : invite.meetingTitle) ?? 'Unavailable',
        meta: `From @${invite.senderUsername ?? 'unknown'}`,
      }}/>
  );
}

export function RequestHistory({timeZone}: { timeZone: string }) {
  return (
    <HistoryCard<JoinRequestResponse>
      views={[{label: 'Received', url: '/api/requests/incoming'}, {label: 'Sent', url: '/api/requests'}]}
      empty='No answered requests here yet.'
      toRow={(request) => request.status === 'PENDING' || request.status === null ? null : {
        key: `request-${request.kind}-${request.id}`,
        kind: request.kind === 'GROUP' ? 'Group' : 'Meeting',
        status: REQUEST_STATUS[request.status],
        title: (request.kind === 'GROUP' ? request.groupName : request.meetingTitle) ?? 'Unavailable',
        meta: [
          `@${request.requesterUsername ?? 'unknown'}`,
          request.respondedAt && formatDateTime(request.respondedAt, timeZone),
        ].filter(Boolean).join(' · '),
      }}/>
  );
}

export function ProposalHistory({timeZone}: { timeZone: string }) {
  return (
    <HistoryCard<TimeChangeProposalResponse>
      views={[
        {label: 'Voted on', url: '/api/meetings/time-changes?role=VOTER'},
        {label: 'Proposed by me', url: '/api/meetings/time-changes?role=PROPOSER'},
      ]}
      empty='No finished time changes here yet.'
      toRow={(proposal) => {
        if (proposal.status === 'PENDING') {
          return null;
        }
        const {day, time} = formatMeetingTime(proposal.proposedStartTime, proposal.proposedEndTime, timeZone);
        return {
          key: `proposal-${proposal.id}`,
          kind: 'Meeting',
          status: PROPOSAL_STATUS[proposal.status],
          title: proposal.meetingTitle ?? 'Meeting',
          meta: `@${proposal.proposedByUsername ?? 'unknown'} proposed ${day}, ${time}`,
        };
      }}/>
  );
}
