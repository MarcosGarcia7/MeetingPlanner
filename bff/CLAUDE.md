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
npm run lint         # eslint (next core-web-vitals + typescript) + stylelint on src/**/*.css
npx tsc --noEmit     # typecheck; there is no test suite yet
```

## Layout

- `src/app/**/page.tsx` — pages. `/dashboard/*` is the signed-in area (friends, groups, inbox,
  profile, schedule, settings) with a shared `layout.tsx` + `SideNav`.
- `src/app/api/**/route.ts` — BFF route handlers the browser calls; each one forwards to the Spring API.
- `src/app/components/<area>/` — components, each with a co-located `*.module.css`.
- `src/lib/session.ts` — Redis session (`sess:<uuid>`, cookie `mp_session`, httpOnly). Idle TTL
  (`SESSION_TTL`) slides on every page and `/api` request: `proxy.ts` extends the Redis key and
  re-sets the cookie (except on `auth/login`/`auth/logout`). The refresh token's lifetime caps it.
- `src/lib/api-client.ts` — `authFetchRaw(path, init, clientIp)`: attaches the bearer token, refreshes
  once on 401, persists rotated tokens, and destroys the session if refresh fails (`AuthError`);
  returns the API `Response` as-is. `authFetch<T>` wraps it, parses JSON, and throws
  `ApiError(status, message, body, retryAfter)` carrying the API's error body. `apiFetch<T>` is the
  same without a session, for auth endpoints called before one exists (login).
- `src/lib/route-handler.ts` — `forward`, `withErrors`, `errorResponse` (see Route handlers below).
- `src/lib/api-error.ts` — `readApiError(response)`: client-side reader for a failed `/api` response's
  `{ error, message }` body (or null). Use it instead of inline `response.json()` casts.
- `src/lib/types/` — TS mirrors of server DTOs (see Types below). No `server-only`, so client
  components can import them.
- `src/lib/dal.ts` — `verifySession()`; call it at the top of server components that need the user.
  It returns only `{ userId, username }` — keep tokens out of anything a page can pass to a client
  component. `session.ts`, `api-client.ts`, and `dal.ts` import `server-only`.
- `src/proxy.ts` — Next 16's replacement for `middleware.ts`: redirects unauthenticated users away
  from `/dashboard` and signed-in users away from `/login`, `/signup`, `/`.

## Route handlers

- **Mirror the API path.** API `/api/groups/{groupId}/members/{userId}` →
  `src/app/api/groups/[groupId]/members/[userId]/route.ts`, one export per method the API supports
  there, same param names. Type the context with the global `RouteContext<'/api/groups/[groupId]'>`
  and `await ctx.params`; `encodeURIComponent` every param when building the API path.
- **Pass-through is the default**: `return forward(request, '/api/...')`. It keeps method, query
  string, JSON body, and client IP, and returns the API's status (200/201/204) and body untouched.
  No BFF-side validation or casting — the API validates.
  ```ts
  export async function PATCH(request: Request, ctx: RouteContext<'/api/groups/[groupId]'>) {
    const { groupId } = await ctx.params;
    return forward(request, `/api/groups/${encodeURIComponent(groupId)}`);
  }
  ```
- **Custom handlers** only when the BFF adds something (transforming, aggregating calls, touching
  the session): `export const GET = withErrors(async (request) => { ... authFetch<T>(...) ... })`.
- Errors are mapped once in `errorResponse`: `ApiError` → its status + the API's body (+
  `Retry-After`); `AuthError` → `401 { error: 'unauthenticated' }`; anything else →
  `500 { error: 'internal_error' }`. UI code branches on `error` codes, never message text.
- Unauthenticated auth endpoints that don't touch the session (`auth/create`, `auth/verify-email`,
  `auth/verify-email/resend`) are plain `forward`s too — with no session it sends no bearer.
- Exceptions to mirroring/forwarding: `auth/login` (picks the email/username endpoint, creates the
  session via `apiFetch`), `auth/logout` (revokes, destroys the session), and BFF-computed resources
  like `avatar/me`.
- Server components call `authFetch` directly rather than going through their own `/api` routes.
- Every route takes and returns JSON; never `formData`.

## Types

- Every DTO the BFF sends or receives — requests and responses — has a type in
  `src/lib/types/<domain>.ts` (domain = server domain: `auth`, `user`, `group`, `meeting`, …), named
  **exactly** as the Java record (`CreateAccountRequest`, `UserProfileResponse`) with a
  `// Mirrors server common/api/dto/X.` comment. Add them when a route gets wired, not ahead of time.
  Update the type together with the server DTO when a contract changes.
- Nullability: in responses, Java reference fields are `T | null`. In requests, fields without
  `@NotBlank`/`@NotNull` are `field?: T | null`; required and primitive fields are plain `T`.
- Shared shapes live in `types/api.ts`: `ApiErrorBody` (`{ error, message }`), `CursorPage<T>`.
- Shapes the BFF defines itself (e.g. `LoginRequest { identifier, password }`) go in the same domain
  file with a `// BFF-only:` comment.
- Type JSON bodies with the DTO (`const body: CreateAccountRequest = { … }` or `satisfies`) and
  responses via `authFetch<T>` / `apiFetch<T>`. No inline `as { message?: string }` casts.

## Forms

- `react-hook-form` + `zod`. The zod schema describes the *form* (it may mirror server constraints
  for instant feedback; the API stays the authority). On submit, map form data to the request DTO
  explicitly — `''` → `undefined` for optional fields — and POST it as JSON.
- On failure, `readApiError(response)`, branch on `error` codes, display `message`; also `catch`
  network errors.

## Styling

- **Tokens** live in `src/app/globals.css` (`--surface`, `--text-primary`, `--accent`, `--border`, …).
  Every token is defined in both the light block and the `prefers-color-scheme: dark` block (derived
  tokens like `--accent-subtle` are written once in terms of others). Modules use only `var(--…)` for
  colors and shadows, and don't add their own dark-mode blocks for them. Need a new color? Add a
  token in both blocks. stylelint rejects hex and named colors outside `globals.css`.
- **Three layers**: tokens → **area module** `components/<area>/<area>.module.css` for primitives
  several components in that area share (e.g. `auth/auth.module.css`: page, card, title, form, field,
  input, error, submit, linkButton) → **component module** for what only that component adds
  (`login-form.module.css` is just `.resendLink`). Check the area module before writing a rule; when
  a second area needs the same rule, move it up into a shared module instead of copying it.
- Variants use `composes:` (`.linkButton { composes: submit; }`); conditional classes use `clsx`.
- Import another area's module only to share its primitives on purpose, never a sibling component's
  module.
- No `style={{…}}` (eslint `react/forbid-dom-props` covers DOM elements; don't pass `style` to
  components like `Link` either).
- A `styles.x` that doesn't exist in the module is just `undefined`. Nothing catches it (no lint
  plugin resolves the `@/` alias), so check new pages in the browser in both color schemes.

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
- When wiring dashboard components to data, server components call `authFetch` directly; client
  components need a BFF route under `src/app/api/` rather than fetching from the client to the API.
- Dates on dashboard pages are formatted in the profile timezone (`'UTC'` when unset) via
  `components/dashboard/home/format.ts`, so server and client render identical text.
- Server endpoints available to wire up are listed in `../API_ENDPOINTS.md`.
