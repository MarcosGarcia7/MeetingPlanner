import {NextResponse} from 'next/server';
import type {NextRequest} from 'next/server';
import {refreshSessionTtl, SESSION_COOKIE, SESSION_COOKIE_OPTIONS} from '@/lib/session';

const protectedRoutes = ['/dashboard'];

const authRoutes = ['/login', '/signup'];

const sessionLifecycleRoutes = ['/api/auth/login', '/api/auth/logout'];

export async function proxy(request: NextRequest) {
  const {pathname} = request.nextUrl;
  if (sessionLifecycleRoutes.includes(pathname)) {
    return NextResponse.next();
  }

  // Every request with a live session counts as activity and slides its idle TTL.
  const sessionId = request.cookies.get(SESSION_COOKIE)?.value;
  const hasSession = sessionId ? await refreshSessionTtl(sessionId) : false;

  const response = route(request, hasSession);
  if (sessionId && hasSession) {
    response.cookies.set(SESSION_COOKIE, sessionId, SESSION_COOKIE_OPTIONS);
  }
  return response;
}

function route(request: NextRequest, hasSession: boolean): NextResponse {
  const {pathname} = request.nextUrl;

  const isProtectedRoute = protectedRoutes.some(
    (route) => pathname === route || pathname.startsWith(`${route}/`)
  );
  const isAuthRoute = authRoutes.some(
    (route) => pathname === route || pathname.startsWith(`${route}/`)
  );

  if (isProtectedRoute && !hasSession) {
    return redirectTo(request, '/login');
  }

  if (isAuthRoute && hasSession) {
    return redirectTo(request, '/dashboard');
  }

  if (pathname === '/' && hasSession) {
    return redirectTo(request, '/dashboard');
  }

  return NextResponse.next();
}

function redirectTo(request: NextRequest, pathname: string): NextResponse {
  const url = request.nextUrl.clone();
  url.pathname = pathname;
  url.search = '';
  return NextResponse.redirect(url);
}

export const config = {
  matcher: [
    '/((?!_next/static|_next/image|favicon.ico|sitemap.xml|robots.txt|.*\\.(?:svg|png|jpg|jpeg|gif|webp|ico)$).*)',
  ],
};
