import 'server-only';

// Time zone for users without a profile zone, from DEFAULT_TIMEZONE: the same variable the API uses
// for emails and messages, so both show the same times. UTC when unset or not a valid IANA zone.
// Read per call (not at module load) so the runtime value applies, not whatever was set at build time.
export function defaultTimeZone(): string {
  const value = process.env.DEFAULT_TIMEZONE?.trim();
  if (value) {
    try {
      new Intl.DateTimeFormat('en-US', {timeZone: value});
      return value;
    } catch {
      // Invalid zone: fall back to UTC rather than break every date on the page.
    }
  }
  return 'UTC';
}

// The zone to render a user's dates in: their profile zone, else the default.
export function profileTimeZone(profile: { timezone: string | null } | null | undefined): string {
  return profile?.timezone || defaultTimeZone();
}
