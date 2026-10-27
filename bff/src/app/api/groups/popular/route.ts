import {forward} from '@/lib/route-handler';

export async function GET(request: Request): Promise<Response> {
  return forward(request, '/api/groups/popular');
}
