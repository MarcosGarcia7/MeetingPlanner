// Mirrors server domains/friend/model/FriendshipStatus.
export type FriendshipStatus = 'PENDING' | 'ACCEPTED' | 'DECLINED' | 'BLOCKED';

// Mirrors server common/api/dto/FriendshipResponse.
export interface FriendshipResponse {
  userId: number;
  username: string;
  firstName: string | null;
  lastName: string | null;
  status: FriendshipStatus;
  requesterId: number;
  createdAt: string;
}
