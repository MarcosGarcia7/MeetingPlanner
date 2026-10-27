import {forward} from '@/lib/route-handler';

export async function POST(request: Request,
                           ctx: RouteContext<'/api/meetings/[meetingId]/invites/check'>): Promise<Response> {
  const {meetingId} = await ctx.params;
  return forward(request, `/api/meetings/${encodeURIComponent(meetingId)}/invites/check`);
}
