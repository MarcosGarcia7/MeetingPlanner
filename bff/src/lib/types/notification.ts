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

// Mirrors server common/api/dto/NotificationPreferencesResponse.
export interface NotificationPreferencesResponse {
  emailEnabled: boolean;
  groupInvites: boolean;
  meetingInvites: boolean;
  meetingReminders: boolean;
  meetingUpdates: boolean;
  friendRequests: boolean;
  postComments: boolean;
  groupPosts: boolean;
  reminderLeadMinutes: number;
}

// Mirrors server common/api/dto/NotificationPreferenceUpdateRequest. Omitted fields stay unchanged.
export interface NotificationPreferenceUpdateRequest {
  emailEnabled?: boolean | null;
  groupInvites?: boolean | null;
  meetingInvites?: boolean | null;
  meetingReminders?: boolean | null;
  meetingUpdates?: boolean | null;
  friendRequests?: boolean | null;
  postComments?: boolean | null;
  groupPosts?: boolean | null;
  reminderLeadMinutes?: number | null;
}
