import {forward} from '@/lib/route-handler';

export async function POST(request: Request,
                           ctx: RouteContext<'/api/meetings/[meetingId]/time-change/[proposalId]/cancel'>): Promise<Response> {
  const {meetingId, proposalId} = await ctx.params;
  return forward(request, `/api/meetings/${encodeURIComponent(meetingId)}/time-change/${encodeURIComponent(proposalId)}/cancel`);
}
