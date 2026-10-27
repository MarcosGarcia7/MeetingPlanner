// BFF-only: shape of GET /api/users/me/notifications/unread-count (the server returns a Map).
export interface UnreadCountResponse {
  unread: number;
}

// Mirrors server common/api/dto/NotificationResponse. `payload` is a JSON object of template variables
// (`requester`, `group`, `title`, `link`, …) whose keys depend on `type`.
export interface NotificationResponse {
  id: number | null;
  type: string | null;
  payload: string | null;
  createdAt: string | null;
  read: boolean;
}
