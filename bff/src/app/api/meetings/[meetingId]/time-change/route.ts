import {forward} from '@/lib/route-handler';

export async function GET(request: Request,
                          ctx: RouteContext<'/api/meetings/[meetingId]/time-change'>): Promise<Response> {
  const {meetingId} = await ctx.params;
  return forward(request, `/api/meetings/${encodeURIComponent(meetingId)}/time-change`);
}
