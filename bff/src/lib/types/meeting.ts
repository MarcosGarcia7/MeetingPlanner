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
