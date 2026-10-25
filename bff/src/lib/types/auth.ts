import type {UserProfileResponse} from './user';

// Mirrors server common/api/dto/CreateAccountRequest.
export interface CreateAccountRequest {
  email: string;
  password: string;
  username?: string | null;
  firstName?: string | null;
  lastName?: string | null;
  isPrivate: boolean;
  timezone?: string | null;
}

// Mirrors server common/api/dto/AccountCreatedResponse.
export interface AccountCreatedResponse {
  email: string;
  message: string;
}

// Mirrors server common/api/dto/LoginEmailRequest.
export interface LoginEmailRequest {
  email: string;
  password: string;
}

// Mirrors server common/api/dto/LoginUsernameRequest.
export interface LoginUsernameRequest {
  username: string;
  password: string;
}

// Mirrors server common/api/dto/TokenResponse. Server-side only: never send tokens to the browser.
export interface TokenResponse {
  accessToken: string;
  refreshToken: string;
  user: UserProfileResponse;
}

// Mirrors server common/api/dto/RefreshRequest.
export interface RefreshRequest {
  refreshToken: string;
}

// Mirrors server common/api/dto/LogoutRequest.
export interface LogoutRequest {
  refreshToken: string;
  allSessions: boolean;
}

// Mirrors server common/api/dto/VerifyEmailRequest.
export interface VerifyEmailRequest {
  token: string;
}

// Mirrors server common/api/dto/VerifyEmailResendRequest.
export interface VerifyEmailResendRequest {
  email: string;
}

// BFF-only: body of POST /api/auth/login; the route picks the email or username endpoint.
export interface LoginRequest {
  identifier: string;
  password: string;
}

// BFF-only: response of POST /api/auth/login (the user, without tokens).
export interface LoginResponse {
  user: UserProfileResponse;
}
