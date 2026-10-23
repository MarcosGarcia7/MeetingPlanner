@AGENTS.md

# MeetingPlanner BFF

Next.js 16 (App Router) + React 19 + TypeScript (strict). Serves the UI and acts as a
backend-for-frontend in front of the Spring API. Sessions in Redis via `ioredis`. Forms use
`react-hook-form` + `zod`. Styling is CSS Modules (no Tailwind). Icons from `@heroicons/react`,
avatars from `@dicebear`.

## Commands

Run from `bff/`. The Dockerfile uses npm with `package-lock.json`; keep that lockfile in sync when
adding dependencies.

```bash
npm run dev          # http://localhost:3000; needs Redis + API (see .env.example)
npm run build        # production build (output: standalone)
npm run lint         # eslint (next core-web-vitals + typescript)
npx tsc --noEmit     # typecheck; there is no test suite yet
```

## Layout

- `src/app/**/page.tsx` — pages. `/dashboard/*` is the signed-in area (friends, groups, inbox,
  profile, schedule, settings) with a shared `layout.tsx` + `SideNav`.
- `src/app/api/**/route.ts` — BFF route handlers the browser calls; each one forwards to the Spring API.
- `src/app/components/<area>/` — components, each with a co-located `*.module.css`.
- `src/lib/session.ts` — Redis session (`sess:<uuid>`, cookie `mp_session`, httpOnly, sliding TTL).
- `src/lib/api-client.ts` — `authFetch<T>(path, init, clientIp)`: attaches the bearer token, refreshes
  once on 401, persists rotated tokens, and destroys the session if refresh fails (`AuthError`).
  Throws `ApiError(status, …)` on other failures.
- `src/lib/dal.ts` — `verifySession()`; call it at the top of server components that need the user.
- `src/proxy.ts` — Next 16's replacement for `middleware.ts`: redirects unauthenticated users away
  from `/dashboard` and signed-in users away from `/login`, `/signup`, `/`.

## Rules

- Tokens never reach the browser. Client components call `/api/...` routes in this app, never the
  Spring API directly; only server code (route handlers, server components) uses `API_INTERNAL_URL`.
- Authenticated API calls go through `authFetch`, not raw `fetch`. Raw `fetch` is only for
  unauthenticated auth endpoints (login, signup, verify-email) that create a session.
- Forward the client IP: `getClientIp(request.headers)` → `X-Forwarded-For` (the API's rate limiting
  and lockout depend on it).
- Pass the API's error body through (`{ error, message }`) with its status so the UI can branch on
  `error` codes such as `email_not_verified`, `account_locked`, `rate_limited`.
- Mark interactive components `'use client'`; keep pages as server components where possible.
- Import with the `@/` alias (`@/lib/...`, `@/app/components/...`).
- Security headers/CSP are set in `next.config.ts`; images must be self-hosted or `data:`/`blob:`.
- Some dashboard components still render placeholder data (e.g. `upcoming-meetings.tsx`); when wiring
  them up, add a BFF route under `src/app/api/` rather than fetching from the client to the API.
- Server endpoints available to wire up are listed in `../API_ENDPOINTS.md`.
