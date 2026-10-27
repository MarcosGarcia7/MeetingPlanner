import {forward} from '@/lib/route-handler';

export async function POST(request: Request,
                           ctx: RouteContext<'/api/requests/meeting/[requestId]/decline'>): Promise<Response> {
  const {requestId} = await ctx.params;
  return forward(request, `/api/requests/meeting/${encodeURIComponent(requestId)}/decline`);
}
