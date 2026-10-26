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
