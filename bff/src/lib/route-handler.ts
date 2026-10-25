import 'server-only';
import {ApiError, AuthError, authFetchRaw, getClientIp} from './api-client';

// Maps anything a route handler throws to the API's { error, message } shape.
export function errorResponse(err: unknown): Response {
  if (err instanceof ApiError) {
    const headers = new Headers();
    if (err.retryAfter) {
      headers.set('Retry-After', err.retryAfter);
    }
    return Response.json(
      err.body ?? {error: `http_${err.status}`, message: 'Request failed'},
      {status: err.status, headers}
    );
  }

  if (err instanceof AuthError) {
    return Response.json(
      {error: 'unauthenticated', message: err.message || 'Please log in again.'},
      {status: 401}
    );
  }

  console.error('Unhandled route handler error', err);
  return Response.json(
    {error: 'internal_error', message: 'Something went wrong'},
    {status: 500}
  );
}

// Wraps a custom route handler so thrown errors become consistent JSON responses.
export function withErrors<Args extends unknown[]>(
  handler: (...args: Args) => Promise<Response>
): (...args: Args) => Promise<Response> {
  return async (...args) => {
    try {
      return await handler(...args);
    } catch (err) {
      return errorResponse(err);
    }
  };
}

const PASSTHROUGH_HEADERS = ['content-type', 'retry-after'];

// Proxies the incoming request to the same-method API endpoint at `apiPath`, keeping the query
// string, JSON body, and client IP, and returns the API's status and body untouched.
export async function forward(request: Request, apiPath: string): Promise<Response> {
  try {
    const {search} = new URL(request.url);
    const hasBody = request.method !== 'GET' && request.method !== 'HEAD';
    const body = hasBody ? await request.text() : '';

    const res = await authFetchRaw(
      `${apiPath}${search}`,
      {
        method: request.method,
        headers: body ? {'Content-Type': 'application/json'} : undefined,
        body: body || undefined,
      },
      getClientIp(request.headers)
    );

    const headers = new Headers();
    for (const name of PASSTHROUGH_HEADERS) {
      const value = res.headers.get(name);
      if (value) headers.set(name, value);
    }
    return new Response(res.body, {status: res.status, headers});
  } catch (err) {
    return errorResponse(err);
  }
}
