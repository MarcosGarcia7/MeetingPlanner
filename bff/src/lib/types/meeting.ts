// Mirrors server common/api/dto/MeetingSummaryResponse.
export interface MeetingSummaryResponse {
  id: number;
  groupId: number | null;
  groupName: string | null;
  creatorId: number;
  startTime: string;
  endTime: string | null;
  location: string | null;
  title: string;
  isPrivate: boolean;
  participantCount: number;
}

// Mirrors server common/api/model/ProposalStatus.
export type ProposalStatus = 'PENDING' | 'APPROVED' | 'REJECTED' | 'EXPIRED' | 'CANCELLED';

// Mirrors server common/api/model/VoteStatus.
export type VoteStatus = 'PENDING' | 'APPROVED' | 'DECLINED';

// Mirrors server common/api/dto/TimeChangeConflict.
export interface TimeChangeConflict {
  meetingId: number;
  title: string;
  startTime: string;
}

// Mirrors server common/api/dto/TimeChangeProposalResponse.
export interface TimeChangeProposalResponse {
  id: number;
  meetingId: number;
  meetingTitle: string | null;
  proposedById: number | null;
  proposedByUsername: string | null;
  proposedStartTime: string;
  proposedEndTime: string | null;
  status: ProposalStatus;
  timeSent: string;
  resolvedAt: string | null;
  myVote: VoteStatus | null;
  conflicts: TimeChangeConflict[];
}

// Mirrors server common/api/dto/TimeChangeVoteRequest.
export interface TimeChangeVoteRequest {
  approve: boolean;
}

// Mirrors server common/api/dto/MeetingCreateRequest.
export interface MeetingCreateRequest {
  startTime: string;
  endTime?: string | null;
  location?: string | null;
  title: string;
  isPrivate?: boolean | null;
}

// Mirrors server common/api/dto/ParticipantResponse.
export interface ParticipantResponse {
  userId: number | null;
  username: string | null;
}

// Mirrors server common/api/dto/PendingTimeChange.
export interface PendingTimeChange {
  proposalId: number | null;
  proposedById: number | null;
  proposedStartTime: string | null;
  proposedEndTime: string | null;
}

// Mirrors server common/api/dto/MeetingResponse.
export interface MeetingResponse {
  id: number | null;
  groupId: number | null;
  creatorId: number | null;
  startTime: string | null;
  endTime: string | null;
  location: string | null;
  title: string | null;
  isPrivate: boolean;
  externalParticipantNames: string[] | null;
  participants: ParticipantResponse[] | null;
  warnings: string[] | null;
  pendingTimeChange: PendingTimeChange | null;
}

// Mirrors server common/api/dto/MeetingInviteRequest. Exactly one of username or email.
export interface MeetingInviteRequest {
  username?: string | null;
  email?: string | null;
}

// Mirrors server common/api/dto/MeetingInviteBatchRequest.
export interface MeetingInviteBatchRequest {
  recipients: MeetingInviteRequest[];
}
