import { getSession, type SessionData } from "./session";

const API_BASE_URL =
  process.env.API_INTERNAL_URL ?? "http://localhost:8080";

export class ApiError extends Error {
  constructor(
    public readonly status: number,
    message: string
  ) {
    super(message);
  }
}

export function getClientIp(headers: Headers): string | undefined {
  const forwarded = headers.get("x-forwarded-for");
  if (!forwarded) return undefined;

  const first = forwarded.split(",")[0].trim().replace(/[\r\n]/g, "");
  return first || undefined;
}

async function request<T>(
  path: string,
  session: SessionData | null,
  init: RequestInit = {},
  clientIp?: string
): Promise<T> {
  const headers = new Headers(init.headers);
  if (session) {
    headers.set("Authorization", `Bearer ${session.accessToken}`);
  }
  if (clientIp) {
    headers.set("X-Forwarded-For", clientIp);
  }

  const res = await fetch(`${API_BASE_URL}${path}`, { ...init, headers });

  if (!res.ok) {
    throw new ApiError(res.status, `API ${path} failed with ${res.status}`);
  }

  if (res.status === 204) {
    return undefined as T;
  }

  return (await res.json()) as T;
}

export async function apiFetch<T>(
  path: string,
  init?: RequestInit,
  clientIp?: string
): Promise<T> {
  const session = await getSession();
  return request<T>(path, session, init, clientIp);
}
