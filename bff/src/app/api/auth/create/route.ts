import { forward } from '@/lib/route-handler';

export async function POST(request: Request): Promise<Response> {
  return forward(request, '/api/auth/create');
}
