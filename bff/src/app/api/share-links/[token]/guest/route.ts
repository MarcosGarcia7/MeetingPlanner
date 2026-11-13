import {forward} from '@/lib/route-handler';

export async function POST(request: Request, ctx: RouteContext<'/api/share-links/[token]/guest'>): Promise<Response> {
  const {token} = await ctx.params;
  return forward(request, `/api/share-links/${encodeURIComponent(token)}/guest`);
}
