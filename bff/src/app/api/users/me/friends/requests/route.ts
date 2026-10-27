import {forward} from '@/lib/route-handler';

export async function POST(request: Request): Promise<Response> {
  return forward(request, '/api/users/me/friends/requests');
}
