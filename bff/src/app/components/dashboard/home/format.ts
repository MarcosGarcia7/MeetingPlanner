// Date helpers for the dashboard. Every formatter takes the user's profile timezone so server and
// client render the same text (no hydration mismatch) regardless of where the code runs.

export function greeting(timeZone?: string): string {
  const hour = Number(
    new Intl.DateTimeFormat('en-US', {hour: 'numeric', hourCycle: 'h23', timeZone}).format(new Date())
  );
  if (hour < 12) return 'Good morning';
  if (hour < 18) return 'Good afternoon';
  return 'Good evening';
}

// "Tue, Oct 28" + "2:00 – 3:30 PM" (end omitted when the meeting has none).
export function formatMeetingTime(
  start: string,
  end: string | null,
  timeZone?: string
): { day: string; time: string } {
  const startDate = new Date(start);
  const day = new Intl.DateTimeFormat('en-US', {weekday: 'short', month: 'short', day: 'numeric', timeZone})
    .format(startDate);
  const timeFormat = new Intl.DateTimeFormat('en-US', {hour: 'numeric', minute: '2-digit', timeZone});
  const time = end ? timeFormat.formatRange(startDate, new Date(end)) : timeFormat.format(startDate);
  return {day, time};
}

const RELATIVE_UNITS: [Intl.RelativeTimeFormatUnit, number][] = [
  ['year', 365 * 24 * 60 * 60],
  ['month', 30 * 24 * 60 * 60],
  ['week', 7 * 24 * 60 * 60],
  ['day', 24 * 60 * 60],
  ['hour', 60 * 60],
  ['minute', 60],
];

// "3 hours ago", "yesterday", "just now". Call from server components only: it depends on the
// current time, which would differ between server render and hydration.
export function formatRelative(iso: string): string {
  const seconds = (new Date(iso).getTime() - Date.now()) / 1000;
  const format = new Intl.RelativeTimeFormat('en-US', {numeric: 'auto'});
  for (const [unit, unitSeconds] of RELATIVE_UNITS) {
    if (Math.abs(seconds) >= unitSeconds) {
      return format.format(Math.round(seconds / unitSeconds), unit);
    }
  }
  return 'just now';
}
