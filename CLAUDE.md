# MeetingPlanner

Meeting planner web app: users, friends, groups, meetings with conflict-aware scheduling, time-change
voting, invites/join requests, notifications, and email.

## Layout

- `server/` — Spring Boot REST API (Java 21, Postgres, Redis). See `server/CLAUDE.md`.
- `bff/` — Next.js backend-for-frontend + UI. See `bff/CLAUDE.md`.
- `docker-compose.yml` — full stack: `db` (Postgres 16), `redis`, `api`, `bff`, `proxy` (Caddy TLS).
- `docker-compose.dev.yml` — publishes Postgres (5434) and Redis (6379) on the host; enabled locally via
  `COMPOSE_FILE` in `.env`, never in production.
- `Caddyfile.local` / `Caddyfile.production` — selected via `CADDYFILE` in `.env`.
- `seed-dev.sql` — manual dev fixtures (alice/bob/unverified, password `Password123!`). Never move it
  into `db/migration`.
- `API_ENDPOINTS.md` — endpoint inventory generated from the Spring controllers. Update it when adding,
  removing, or renaming endpoints.

The root `package.json` / `node_modules` are vestigial; the real Node project is `bff/`.

## Request flow

Browser → Caddy (`https://localhost`) → BFF (`:3000`) → API (`:8080`, internal network only).

The browser never talks to the API or sees JWTs. The BFF holds access/refresh tokens in a Redis session
keyed by an httpOnly `mp_session` cookie, and calls the API with `Authorization: Bearer …`. The API is
stateless and trusts only the JWT.

## Running

```bash
docker compose up --build        # whole stack; Flyway migrates on API startup
docker compose up db redis       # just infra, then run server/bff on the host
```

First-time setup (DB creation, mkcert certs, seeding) is in `README.md`. With `EMAIL_ENABLED=false`,
emails are not sent but still land in the `email_outbox` table — read verification/invite links from
there.

## Environment

`.env` at the repo root (gitignored, never commit or print secrets from it) drives Docker Compose and
also the server when run from the repo root via spring-dotenv. `server/.env.example` and
`bff/.env.example` show the values for running each app directly on the host.

## Conventions

- Commit messages: past-tense summary sentences ("Implemented …", "Added …").
- When an API contract changes, update the server DTO, the BFF route/types that consume it, and
  `API_ENDPOINTS.md` together.
