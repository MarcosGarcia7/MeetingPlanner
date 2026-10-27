import {forward} from '@/lib/route-handler';

export async function DELETE(request: Request,
                             ctx: RouteContext<'/api/meetings/[meetingId]/participants/me'>): Promise<Response> {
  const {meetingId} = await ctx.params;
  return forward(request, `/api/meetings/${encodeURIComponent(meetingId)}/participants/me`);
}
