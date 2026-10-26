// BFF-only: shape of GET /api/users/me/notifications/unread-count (the server returns a Map).
export interface UnreadCountResponse {
  unread: number;
}
