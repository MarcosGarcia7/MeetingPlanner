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
  firstName: string | null;
  lastName: string | null;
  avatarColor: string | null;
  isAdmin: boolean;
}

// Mirrors server common/api/dto/GroupResponse. `members` is empty and `viewerShareSchedule` null unless
// the viewer is a member.
export interface GroupResponse {
  id: number | null;
  groupName: string | null;
  ownerId: number | null;
  isPrivate: boolean;
  memberCount: number;
  membersCanPost: boolean;
  commentsEnabled: boolean;
  membersCanCreate: boolean;
  members: GroupMemberResponse[] | null;
  viewerShareSchedule: boolean | null;
}

// Mirrors server common/api/dto/GroupCreateRequest.
export interface GroupCreateRequest {
  groupName: string;
  isPrivate?: boolean | null;
}

// Mirrors server common/api/dto/PostCreateRequest.
export interface PostCreateRequest {
  body: string;
  commentsEnabled?: boolean | null;
}

// Mirrors server common/api/dto/PostCommentResponse.
export interface PostCommentResponse {
  id: number;
  postId: number;
  authorId: number;
  authorUsername: string | null;
  body: string;
  createdAt: string;
}

// Mirrors server common/api/dto/PostCommentCreateRequest.
export interface PostCommentCreateRequest {
  body: string;
}

// Mirrors server common/api/dto/CommentsEnabledRequest.
export interface CommentsEnabledRequest {
  commentsEnabled: boolean;
}

// Mirrors server common/api/dto/GroupUpdateRequest. Omitted fields stay as they are; groupName and
// isPrivate are owner-only.
export interface GroupUpdateRequest {
  groupName?: string | null;
  isPrivate?: boolean | null;
  membersCanPost?: boolean | null;
  commentsEnabled?: boolean | null;
  membersCanCreate?: boolean | null;
}

// Mirrors server common/api/dto/GroupPrivacyRequest.
export interface GroupPrivacyRequest {
  shareSchedule: boolean;
}

// Mirrors server common/api/dto/MemberUsernameRequest.
export interface MemberUsernameRequest {
  username: string;
}

// Mirrors server common/api/dto/GroupInviteBatchRequest.
export interface GroupInviteBatchRequest {
  recipients: MemberUsernameRequest[];
}

// Mirrors server common/api/dto/GroupBanResponse.
export interface GroupBanResponse {
  userId: number | null;
  username: string | null;
  bannedById: number | null;
  bannedByUsername: string | null;
  bannedByOwner: boolean;
  bannedAt: string | null;
}
