// Mirrors server domains/friend/model/FriendshipStatus.
export type FriendshipStatus = 'PENDING' | 'ACCEPTED' | 'DECLINED' | 'BLOCKED';

// Mirrors server common/api/dto/FriendshipResponse. The user fields describe the other person.
export interface FriendshipResponse {
  userId: number;
  username: string;
  firstName: string | null;
  lastName: string | null;
  avatarColor: string | null;
  status: FriendshipStatus;
  requesterId: number;
  createdAt: string;
}

// Mirrors server common/api/model/UserRelationship. Declined and blocked pairs report NONE.
export type UserRelationship = 'NONE' | 'FRIENDS' | 'REQUEST_SENT' | 'REQUEST_RECEIVED';

// Mirrors server common/api/dto/UserSearchResult.
export interface UserSearchResult {
  id: number;
  username: string;
  firstName: string | null;
  lastName: string | null;
  avatarColor: string | null;
  relationship: UserRelationship;
}
