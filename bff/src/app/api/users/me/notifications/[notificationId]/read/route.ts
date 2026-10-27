import {forward} from '@/lib/route-handler';

export async function POST(
  request: Request,
  ctx: RouteContext<'/api/users/me/notifications/[notificationId]/read'>
): Promise<Response> {
  const {notificationId} = await ctx.params;
  return forward(request, `/api/users/me/notifications/${encodeURIComponent(notificationId)}/read`);
}
