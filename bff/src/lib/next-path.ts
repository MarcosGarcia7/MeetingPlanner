// Validates a `?next=` return path so login can only send users back into this app: it must be a
// same-origin absolute path ("/x"), not protocol-relative ("//host") or a backslash variant that
// browsers treat as one ("/\host").
export function safeNextPath(value: string | string[] | null | undefined): string | null {
  if (typeof value !== 'string') return null;
  if (!value.startsWith('/') || value.startsWith('//') || value.startsWith('/\\')) return null;
  return value;
}

export function loginHref(next: string): string {
  return `/login?next=${encodeURIComponent(next)}`;
}
