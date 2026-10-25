import {forward} from '@/lib/route-handler';

export async function GET(request: Request): Promise<Response> {
  return forward(request, '/api/users/me/availability');
}

export async function PUT(request: Request): Promise<Response> {
  return forward(request, '/api/users/me/availability');
}
