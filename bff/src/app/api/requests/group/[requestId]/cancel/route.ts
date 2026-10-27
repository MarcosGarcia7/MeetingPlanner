import {forward} from '@/lib/route-handler';

export async function POST(
  request: Request,
  ctx: RouteContext<'/api/requests/group/[requestId]/cancel'>
): Promise<Response> {
  const {requestId} = await ctx.params;
  return forward(request, `/api/requests/group/${encodeURIComponent(requestId)}/cancel`);
}
