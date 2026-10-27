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
