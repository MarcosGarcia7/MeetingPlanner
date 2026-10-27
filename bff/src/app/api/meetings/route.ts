import {forward} from '@/lib/route-handler';

export async function GET(request: Request): Promise<Response> {
  return forward(request, '/api/meetings');
}

export async function POST(request: Request): Promise<Response> {
  return forward(request, '/api/meetings');
}
