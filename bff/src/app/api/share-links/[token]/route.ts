import {forward} from '@/lib/route-handler';

// Public on the API; a signed-in caller's bearer also tells it whether they're already a member.
export async function GET(request: Request, ctx: RouteContext<'/api/share-links/[token]'>): Promise<Response> {
  const {token} = await ctx.params;
  return forward(request, `/api/share-links/${encodeURIComponent(token)}`);
}
