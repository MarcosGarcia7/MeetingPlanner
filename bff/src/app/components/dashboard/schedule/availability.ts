import type {UserAvailabilityResponse} from '@/lib/types/user';

// Weekly availability as the API stores it: per day, [startMinute, endMinute) windows from 0 to 1440,
// read in the user's profile timezone.

export const DAYS = ['sunday', 'monday', 'tuesday', 'wednesday', 'thursday', 'friday', 'saturday'] as const;
export type Day = typeof DAYS[number];

export const DAY_LABELS: Record<Day, string> = {
  sunday: 'Sunday',
  monday: 'Monday',
  tuesday: 'Tuesday',
  wednesday: 'Wednesday',
  thursday: 'Thursday',
  friday: 'Friday',
  saturday: 'Saturday',
};

export const WEEKDAYS: Day[] = ['monday', 'tuesday', 'wednesday', 'thursday', 'friday'];

export const DAY_END = 1440;

export type Windows = Record<Day, number[][]>;

export function toWindows(availability: UserAvailabilityResponse | null): Windows {
  return Object.fromEntries(DAYS.map((day) => [day, availability?.[day] ?? []])) as Windows;
}

// Minutes → "HH:MM" for <input type="time">. The end of the day (1440) shows as "00:00".
export function toTimeInput(minutes: number): string {
  const wrapped = minutes % DAY_END;
  return `${String(Math.floor(wrapped / 60)).padStart(2, '0')}:${String(wrapped % 60).padStart(2, '0')}`;
}

// "HH:MM" → minutes, or null when the input is empty. An end of "00:00" means midnight at the day's end.
export function fromTimeInput(value: string, isEnd: boolean): number | null {
  const match = /^(\d{2}):(\d{2})$/.exec(value);
  if (!match) {
    return null;
  }
  const minutes = Number(match[1]) * 60 + Number(match[2]);
  return isEnd && minutes === 0 ? DAY_END : minutes;
}

// "9:00 AM – 5:00 PM", or "All day". Formatted from fixed UTC dates so the result doesn't depend on any
// timezone.
export function formatWindow([start, end]: number[]): string {
  if (start === 0 && end >= DAY_END) {
    return 'All day';
  }
  const at = (minutes: number) => new Date(Date.UTC(2000, 0, 1, 0, minutes));
  return new Intl.DateTimeFormat('en-US', {hour: 'numeric', minute: '2-digit', timeZone: 'UTC'})
    .formatRange(at(start), at(end));
}

// The first problem with a day's windows, or null when they're valid (each end after its start, no overlaps).
export function windowError(windows: number[][]): string | null {
  if (windows.some(([start, end]) => end <= start)) {
    return 'Each end time must be after its start.';
  }
  const sorted = [...windows].sort((a, b) => a[0] - b[0]);
  if (sorted.some((window, i) => i > 0 && window[0] < sorted[i - 1][1])) {
    return 'These hours overlap.';
  }
  return null;
}

export function sortWindows(windows: number[][]): number[][] {
  return [...windows].sort((a, b) => a[0] - b[0]);
}
