import type {TargetKind} from './api';

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
  warnings: string[];
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
