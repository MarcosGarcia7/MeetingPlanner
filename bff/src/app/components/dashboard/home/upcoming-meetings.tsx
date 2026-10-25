'use client';

import Table, {type Column} from '@/app/components/common/table';

interface Meeting {
  id: number;
  name: string;
  creator: string;
}

export default function UpcomingMeetings() {

  const meetings: Meeting[] = [
    {id: 1, name: 'meeting', creator: 'me'},
    {id: 2, name: 'meeting', creator: 'me'},
    {id: 3, name: 'meeting', creator: 'me'},
    {id: 4, name: 'meeting', creator: 'me'},
    {id: 5, name: 'meeting', creator: 'me'},
    {id: 6, name: 'meeting', creator: 'me'},
  ];

  return (
    <Table<Meeting>
      title='Upcoming Meetings'
      columns={[
        {key: 'name', header: 'Name', type: 'string'},
        {key: 'creator', header: 'Creator', type: 'string'},
      ]}
      data={meetings}
    />
  )
}
