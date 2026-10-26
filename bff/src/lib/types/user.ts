// Mirrors server common/api/dto/UserProfileResponse.
export interface UserProfileResponse {
  id: number;
  username: string;
  email: string;
  firstName: string | null;
  lastName: string | null;
  avatarColor: string;
  timezone: string | null;
  isPrivate: boolean;
  friendsCanViewSchedule: boolean;
  emailVerified: boolean;
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
