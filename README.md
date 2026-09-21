# MeetingPlanner
Meeting planner web app.

## Local development & seeding

### Prerequisites

- Docker (for Redis)
- Node.js + pnpm (BFF)
- JDK 21 (API)
- PostgreSQL 18 running locally on `localhost:5432` (the API connects to `meeting_planner_db`)

Copy `.env.example` to `.env` and fill in the values (the BFF reads `bff/.env`).

### 1. Start Redis

The compose `redis` service is published on the host so the native API and BFF can reach it:

```bash
docker compose up -d redis
```

### 2. Start the API

Run `com.meetingplanner.MeetingPlannerApplication` with the `.env` values available as environment
variables (e.g. an IntelliJ run configuration). On startup Flyway applies the migrations in
`server/src/main/resources/db/migration`, creating the schema. Confirm with `\dt` in `psql`.

### 3. Start the BFF

```bash
pnpm dev
```

Open http://localhost:3000.

### 4. Seed the database

Seed **after** the API has run once (so the migrations exist and `pgcrypto` is installed). From the
repo root:

```powershell
$env:PGPASSWORD = (Select-String -Path .env -Pattern '^DB_PASSWORD=').Line.Split('=',2)[1]
psql -h localhost -U postgres -d meeting_planner_db -f seed-dev.sql
```

This creates three fixtures, all with the password `Password123!`:

| Email                     | Username     | Verified |
| ------------------------- | ------------ | -------- |
| `alice@example.com`       | `alice`      | yes      |
| `bob@example.com`         | `bob`        | yes      |
| `unverified@example.com`  | `unverified` | no       |

Sign in at http://localhost:3000/login. The `unverified` account exercises the
`email_not_verified` branch (including the resend link).

### Testing signup without email delivery

`EMAIL_ENABLED=false` means no mail is sent, but the verification link is still queued in the
`email_outbox` table. After signing up, retrieve it with:

```sql
SELECT payload->>'link' FROM email_outbox WHERE template = 'EMAIL_VERIFY' ORDER BY created_at DESC LIMIT 1;
```

Open the link to verify the account. Set `APP_BASE_URL=http://localhost:3000` in `.env` first so the
link points at the local BFF.

### Notes

- The API runs Flyway on startup, so the schema must be created by starting the API at least once.
- Keep `seed-dev.sql` out of `db/migration` — it is a manual, development-only script.
- If the session does not persist after login over plain HTTP, set `COOKIE_SECURE=false` in
  `bff/.env`.
