import {forward} from '@/lib/route-handler';

export async function GET(request: Request): Promise<Response> {
  return forward(request, '/api/users/me/friends/blocked');
}

export async function POST(request: Request): Promise<Response> {
  return forward(request, '/api/users/me/friends/blocked');
}
