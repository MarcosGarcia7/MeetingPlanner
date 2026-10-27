// Mirrors server common/api/dto/PostResponse.
export interface PostResponse {
  id: number;
  groupId: number;
  groupName: string | null;
  authorId: number;
  authorUsername: string | null;
  body: string;
  commentsEnabled: boolean;
  createdAt: string;
}

// Mirrors server common/api/model/GroupViewerStatus.
export type GroupViewerStatus = 'MEMBER' | 'BANNED' | 'INVITED' | 'REQUESTED' | 'NONE';

// Mirrors server common/api/dto/GroupSummaryResponse.
export interface GroupSummaryResponse {
  id: number;
  groupName: string | null;
  ownerId: number | null;
  isPrivate: boolean;
  memberCount: number;
  friendMemberCount: number;
  viewerStatus: GroupViewerStatus | null;
  pendingRequestId: number | null;
  pendingInviteId: number | null;
}

// Mirrors server common/api/dto/GroupMemberResponse.
export interface GroupMemberResponse {
  userId: number | null;
  username: string | null;
  isAdmin: boolean;
}

// Mirrors server common/api/dto/GroupResponse. `members` is empty unless the viewer is a member.
export interface GroupResponse {
  id: number | null;
  groupName: string | null;
  ownerId: number | null;
  isPrivate: boolean;
  memberCount: number;
  members: GroupMemberResponse[] | null;
}

// Mirrors server common/api/dto/GroupCreateRequest.
export interface GroupCreateRequest {
  groupName: string;
  isPrivate?: boolean | null;
}
