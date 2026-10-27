// Mirrors server common/api/model/RequestStatus.
export type RequestStatus = 'PENDING' | 'APPROVED' | 'DECLINED' | 'CANCELLED';

// Mirrors server common/api/dto/JoinRequestResponse.
export interface JoinRequestResponse {
  id: number | null;
  kind: string | null;
  status: RequestStatus | null;
  requesterId: number | null;
  requesterUsername: string | null;
  groupId: number | null;
  groupName: string | null;
  meetingId: number | null;
  meetingTitle: string | null;
  timeSent: string | null;
  respondedAt: string | null;
}
