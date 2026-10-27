import {forward} from '@/lib/route-handler';

export async function DELETE(request: Request,
                             ctx: RouteContext<'/api/groups/[groupId]/posts/[postId]'>): Promise<Response> {
  const {groupId, postId} = await ctx.params;
  return forward(request, `/api/groups/${encodeURIComponent(groupId)}/posts/${encodeURIComponent(postId)}`);
}
