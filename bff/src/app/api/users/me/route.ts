import {forward} from '@/lib/route-handler';

export async function GET(request: Request): Promise<Response> {
  return forward(request, '/api/users/me');
}

export async function PATCH(request: Request): Promise<Response> {
  return forward(request, '/api/users/me');
}
