import {forward} from '@/lib/route-handler';

export async function DELETE(request: Request,
                             ctx: RouteContext<'/api/meetings/[meetingId]/participants/[userId]'>): Promise<Response> {
  const {meetingId, userId} = await ctx.params;
  return forward(request, `/api/meetings/${encodeURIComponent(meetingId)}/participants/${encodeURIComponent(userId)}`);
}
