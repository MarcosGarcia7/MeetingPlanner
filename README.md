# MeetingPlanner
Meeting planner web app.

## Local development & seeding

### Prerequisites

- Docker with Docker Compose
- [mkcert](https://github.com/FiloSottile/mkcert) (for local HTTPS)

Copy `.env.example` to `.env`. It already defaults to a local HTTPS setup (`https://localhost`); the
production values are included commented out in the file.

### 1. Start the database

```bash
docker compose up db
```

In a separate terminal, open a `psql` session inside the running container and create the database:

```bash
docker exec -it postgres_db psql -U postgres
```

```sql
CREATE DATABASE meeting_planner_db;
```

### 2. Generate local TLS certificates

The `proxy` service uses `Caddyfile.local`, which serves `certs/localhost.pem` and
`certs/localhost-key.pem`. That folder is gitignored, so generate the certificate once from the repo
root:

```bash
mkcert -install
mkdir certs
mkcert -cert-file certs/localhost.pem -key-file certs/localhost-key.pem localhost 127.0.0.1 ::1
```

### 3. Start the app

```bash
docker compose up --build
```

This builds and starts the API, BFF, Redis, and TLS proxy. On startup Flyway applies the migrations
in `server/src/main/resources/db/migration`, creating the schema.

Open https://localhost.

### 4. Seed the database

Seed **after** the app has started and the migrations have run (so the schema exists and `pgcrypto`
is installed). Open a `psql` session against the created database and run/paste in `seed-dev.sql`:

```bash
docker exec -it postgres_db psql -U postgres -d meeting_planner_db
```

```sql
-- paste the contents of seed-dev.sql here
```

This creates three fixtures, all with the password `Password123!`:

| Email                     | Username     | Verified |
| ------------------------- | ------------ | -------- |
| `alice@example.com`       | `alice`      | yes      |
| `bob@example.com`         | `bob`        | yes      |
| `unverified@example.com`  | `unverified` | no       |

Sign in at https://localhost/login. The `unverified` account exercises the
`email_not_verified` branch (including the resend link).

### Testing signup without email delivery

`EMAIL_ENABLED=false` means no mail is sent, but the verification link is still queued in the
`email_outbox` table. After signing up, retrieve it with:

```sql
SELECT payload->>'link' FROM email_outbox WHERE template = 'EMAIL_VERIFY' ORDER BY created_at DESC LIMIT 1;
```

Open the link to verify the account. Make sure `APP_BASE_URL=https://localhost` in `.env` so the link
points at the local stack.

### Notes

- The API runs Flyway on startup, so the schema is created the first time the stack starts.
- Keep `seed-dev.sql` out of `db/migration` — it is a manual, development-only script.
- `node_modules/` is gitignored by design and not needed locally: `docker compose up --build`
  installs dependencies inside the BFF image from the tracked `bff/package-lock.json`. Only run
  `pnpm install` if you want to run the BFF outside Docker.
