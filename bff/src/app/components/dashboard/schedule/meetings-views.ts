// The Meetings tab's filters and the list route behind each; shared by the page (first page) and the
// client list (further pages).

export type MeetingsView = 'upcoming' | 'past' | 'created';

export const VIEWS: { view: MeetingsView; label: string; url: string; empty: string }[] = [
  {view: 'upcoming', label: 'Upcoming', url: '/api/meetings?when=UPCOMING', empty: 'Nothing scheduled.'},
  {view: 'past', label: 'Past', url: '/api/meetings?when=PAST', empty: 'No past meetings.'},
  {view: 'created', label: 'Created by me', url: '/api/meetings/created', empty: 'You haven’t scheduled any meetings.'},
];

export function listUrl(view: MeetingsView): string {
  return VIEWS.find((v) => v.view === view)!.url;
}
