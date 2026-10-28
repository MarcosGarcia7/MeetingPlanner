// Mirrors server common/api/dto/UserProfileResponse.
export interface UserProfileResponse {
  id: number;
  username: string;
  email: string | null;
  // Address awaiting confirmation via the EMAIL_CHANGE link; only returned on your own profile.
  pendingEmail: string | null;
  firstName: string | null;
  lastName: string | null;
  avatarColor: string;
  timezone: string | null;
  isPrivate: boolean;
  friendsCanViewSchedule: boolean;
  emailVerified: boolean;
}

// Mirrors server common/api/dto/UserProfileUpdateRequest. Omitted/null fields are left unchanged; a
// pendingEmail equal to the current email cancels a pending change.
export interface UserProfileUpdateRequest {
  firstName?: string | null;
  lastName?: string | null;
  avatarColor?: string | null;
  timezone?: string | null;
  isPrivate?: boolean | null;
  friendsCanViewSchedule?: boolean | null;
  pendingEmail?: string | null;
}

// Mirrors server common/api/dto/DeleteAccountRequest.
export interface DeleteAccountRequest {
  currentPassword: string;
}

// Mirrors server common/api/dto/ChangePasswordRequest.
export interface ChangePasswordRequest {
  currentPassword: string;
  newPassword: string;
}

// Mirrors server common/api/dto/UserAvailabilityUpdateRequest. Each day is a list of
// [startMinute, endMinute] windows (0–1440); a null or omitted day is left unchanged.
export interface UserAvailabilityUpdateRequest {
  sunday?: number[][] | null;
  monday?: number[][] | null;
  tuesday?: number[][] | null;
  wednesday?: number[][] | null;
  thursday?: number[][] | null;
  friday?: number[][] | null;
  saturday?: number[][] | null;
  availabilitySet: boolean;
}

// Mirrors server common/api/dto/UserAvailabilityResponse.
export interface UserAvailabilityResponse {
  id: number | null;
  sunday: number[][];
  monday: number[][];
  tuesday: number[][];
  wednesday: number[][];
  thursday: number[][];
  friday: number[][];
  saturday: number[][];
  availabilitySet: boolean;
}
