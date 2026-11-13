import type {TargetKind} from './api';
import type {ScheduleConflict} from './meeting';

// Mirrors server common/api/model/InviteStatus.
export type InviteStatus = 'PENDING' | 'ACCEPTED' | 'DECLINED' | 'LEFT';

// Mirrors server common/api/dto/InviteResponse.
export interface InviteResponse {
  id: number;
  kind: TargetKind;
  status: InviteStatus;
  senderId: number;
  senderUsername: string | null;
  recipientId: number | null;
  recipientEmail: string | null;
  groupId: number | null;
  groupName: string | null;
  meetingId: number | null;
  meetingTitle: string | null;
  meetingStartTime: string | null;
  meetingEndTime: string | null;
  meetingLocation: string | null;
  // Heads-ups for the sender; set on invites just sent.
  warnings: string[];
  // The recipient's meetings this one overlaps; set on pending meeting invites listed for the recipient.
  conflicts: ScheduleConflict[];
}

// Mirrors server common/api/dto/InviteFailure.
export interface InviteFailure {
  username: string | null;
  email: string | null;
  reason: string | null;
}

// Mirrors server common/api/dto/InviteCheckResult.
export interface InviteCheckResult {
  username: string | null;
  email: string | null;
  warnings: string[] | null;
}

// Mirrors server common/api/dto/InviteCheckResponse.
export interface InviteCheckResponse {
  recipients: InviteCheckResult[] | null;
}

// Mirrors server common/api/dto/InviteBatchResponse. The API answers 400 instead when every recipient failed (its message lists each recipient's reason).
export interface InviteBatchResponse {
  invited: InviteResponse[];
  failed: InviteFailure[];
}

// Mirrors server common/api/model/ShareLinkJoinMode.
export type ShareLinkJoinMode = 'DIRECT' | 'REQUEST';

// Mirrors server common/api/model/ShareLinkJoinOutcome.
export type ShareLinkJoinOutcome = 'JOINED' | 'REQUESTED' | 'ALREADY_MEMBER';

// Mirrors server common/api/dto/ShareLinkResponse. joinMode is null for meeting links; usable is false once
// the link expired, hit its use limit, or its creator lost the right to share it.
export interface ShareLinkResponse {
  token: string;
  url: string;
  joinMode: ShareLinkJoinMode | null;
  expiresAt: string | null;
  maxUses: number | null;
  useCount: number;
  createdAt: string;
  createdByUsername: string | null;
  usable: boolean;
}

// Mirrors server common/api/dto/CreateGroupShareLinkRequest.
export interface CreateGroupShareLinkRequest {
  joinMode: ShareLinkJoinMode;
  expiresInDays?: number | null;
  maxUses?: number | null;
}

// Mirrors server common/api/dto/CreateMeetingShareLinkRequest.
export interface CreateMeetingShareLinkRequest {
  expiresInDays?: number | null;
  maxUses?: number | null;
}

// Mirrors server common/api/dto/ShareLinkPreviewResponse. viewerIsMember is null for anonymous viewers.
export interface ShareLinkPreviewResponse {
  kind: TargetKind;
  groupId: number | null;
  groupName: string | null;
  meetingId: number | null;
  meetingTitle: string | null;
  startTime: string | null;
  endTime: string | null;
  location: string | null;
  createdByUsername: string | null;
  joinMode: ShareLinkJoinMode | null;
  viewerIsMember: boolean | null;
}

// Mirrors server common/api/dto/ShareLinkJoinResponse.
export interface ShareLinkJoinResponse {
  outcome: ShareLinkJoinOutcome;
  kind: TargetKind;
  groupId: number | null;
  meetingId: number | null;
  requestId: number | null;
}

// Mirrors server common/api/dto/ShareLinkGuestRequest.
export interface ShareLinkGuestRequest {
  email: string;
}
