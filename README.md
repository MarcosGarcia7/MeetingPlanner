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

### Email in production (Resend over SMTP)

1. Create a Resend account, add a sending domain, and add the DNS records
   it shows (SPF/MX + DKIM). Wait until the domain is "Verified".
2. Add a DMARC record: `_dmarc.yourdomain.com` TXT `v=DMARC1; p=none; rua=mailto:you@yourdomain.com`.
3. Create an API key with "Sending access" only.
4. Set in `.env`:

   ```
   EMAIL_ENABLED=true
   MAIL_SMTP_HOST=smtp.resend.com
   MAIL_SMTP_PORT=587
   MAIL_SMTP_USERNAME=resend
   MAIL_SMTP_PASSWORD=<api key>
   MAIL_FROM=MeetingPlanner <no-reply@mail.yourdomain.com>
   APP_BASE_URL=https://yourdomain.com
   ```

   If your host blocks outbound port 587, use `2587`. Any other SMTP provider (Postmark, SES, …) works
   by changing these values only.
5. Check delivery with
   `SELECT status, attempts, last_error FROM email_outbox ORDER BY created_at DESC LIMIT 5;`. Failures
   are also logged as `event=email_send_failed`.

## Deploying to a VPS

The whole stack runs on one Linux server with Docker Compose; Caddy obtains the TLS certificate
automatically. A VPS with 2 vCPU / 4 GB RAM (e.g. Hetzner Cloud, DigitalOcean, AWS Lightsail) running
Ubuntu 24.04 is enough.

1. Point an `A` (and optionally `AAAA`) record for your domain at the server's IP.
2. Install Docker and allow only SSH, HTTP, and HTTPS through the firewall:

   ```bash
   curl -fsSL https://get.docker.com | sh
   ufw allow OpenSSH && ufw allow 80 && ufw allow 443 && ufw enable
   ```

   Docker-published ports bypass `ufw`, which is why Postgres and Redis are only published by
   `docker-compose.dev.yml`. Do not set `COMPOSE_FILE` on the server.
3. Clone the repo and create `.env` from `.env.example` on the server (never commit it). Use the
   production block (`APP_DOMAIN`, `JWT_ISSUER`, `APP_BASE_URL`, `CADDYFILE=./Caddyfile.production`),
   drop the `COMPOSE_*` lines, generate fresh secrets (`openssl rand -base64 48`), and configure
   email as described above.
4. Start the stack and create the database the first time, as in local steps 1 and 3:

   ```bash
   docker compose up -d --build
   ```

   Do not run `seed-dev.sql` in production.
5. Deploy updates with `git pull && docker compose up -d --build`.
6. Back up Postgres nightly and copy the dumps off the server, e.g. via cron:

   ```bash
   docker exec postgres_db pg_dump -U postgres -Fc meeting_planner_db > /backups/mp-$(date +%F).dump
   ```

### Notes

- The API runs Flyway on startup, so the schema is created the first time the stack starts.
- Keep `seed-dev.sql` out of `db/migration` — it is a manual, development-only script.
- `node_modules/` is gitignored by design and not needed locally: `docker compose up --build`
  installs dependencies inside the BFF image from the tracked `bff/package-lock.json`. Only run
  `npm install` (from `bff/`) if you want to run the BFF outside Docker.
