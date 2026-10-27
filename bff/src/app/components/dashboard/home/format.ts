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

// "Oct 28, 2026, 2:00 PM": an absolute timestamp, safe to render on both server and client.
export function formatDateTime(iso: string, timeZone?: string): string {
  return new Intl.DateTimeFormat('en-US', {dateStyle: 'medium', timeStyle: 'short', timeZone})
    .format(new Date(iso));
}

// Milliseconds the zone is ahead of UTC at `date`.
function zoneOffset(date: Date, timeZone: string): number {
  const parts = new Intl.DateTimeFormat('en-US', {
    timeZone, hourCycle: 'h23', year: 'numeric', month: '2-digit', day: '2-digit',
    hour: '2-digit', minute: '2-digit', second: '2-digit',
  }).formatToParts(date);
  const value = (type: Intl.DateTimeFormatPartTypes) => Number(parts.find((part) => part.type === type)?.value);
  const wallClock = Date.UTC(value('year'), value('month') - 1, value('day'), value('hour'), value('minute'),
    value('second'));
  return wallClock - Math.floor(date.getTime() / 1000) * 1000;
}

// Reads a <input type="datetime-local"> value ("2026-10-28T14:00") as wall-clock time in `timeZone`
// (the profile timezone, not the browser's) and returns it as an ISO instant for the API.
export function zonedTimeToIso(local: string, timeZone: string): string {
  const asUtc = Date.parse(`${local}:00Z`);
  const guess = asUtc - zoneOffset(new Date(asUtc), timeZone);
  // A second pass corrects the offset when the guess lands on the other side of a DST change.
  return new Date(asUtc - zoneOffset(new Date(guess), timeZone)).toISOString();
}

// The inverse of zonedTimeToIso: an ISO instant as a <input type="datetime-local"> value ("2026-10-28T14:00")
// showing its wall-clock time in `timeZone`.
export function isoToZonedInput(iso: string, timeZone: string): string {
  const parts = new Intl.DateTimeFormat('en-US', {
    timeZone, hourCycle: 'h23', year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit',
  }).formatToParts(new Date(iso));
  const value = (type: Intl.DateTimeFormatPartTypes) => parts.find((part) => part.type === type)?.value;
  return `${value('year')}-${value('month')}-${value('day')}T${value('hour')}:${value('minute')}`;
}

// "Tuesday, October 28, 2026": the full date of a meeting's start, for its detail page.
export function formatLongDate(iso: string, timeZone?: string): string {
  return new Intl.DateTimeFormat('en-US', {dateStyle: 'full', timeZone}).format(new Date(iso));
}
