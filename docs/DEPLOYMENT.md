# Deployment

The whole stack runs on one Linux server with Docker Compose, and Caddy obtains the TLS certificate
automatically. A VPS with 2 vCPU and 4 GB RAM running Ubuntu 24.04 is enough (for example Hetzner
Cloud, DigitalOcean, or AWS Lightsail).

## 1. Prepare the server

1. Point an `A` record (and optionally `AAAA`) for your domain at the server's IP.

2. Install Docker, and only allow SSH, HTTP, and HTTPS through the firewall:

   ```bash
   curl -fsSL https://get.docker.com | sh
   ufw allow OpenSSH && ufw allow 80 && ufw allow 443 && ufw enable
   ```

   Docker-published ports bypass `ufw`, which is why only `docker-compose.dev.yml` publishes Postgres
   and Redis. Don't set `COMPOSE_FILE` on the server.

## 2. Configure the environment

Clone the repo and create `.env` from `.env.example` on the server. Never commit this file. Then:

1. Comment out the local development block, including the `COMPOSE_*` lines.
2. Set the production values:

   ```env
   APP_DOMAIN=yourdomain.com
   CADDYFILE=./Caddyfile.production
   JWT_ISSUER=https://yourdomain.com
   APP_BASE_URL=https://yourdomain.com
   ```

3. Replace `DB_PASSWORD`, `REDIS_PASSWORD`, `JWT_ACCESS_SECRET`, and `JWT_REFRESH_SECRET` with fresh
   values. You can generate each one with:

   ```bash
   openssl rand -base64 48
   ```

4. [Set up email](#5-set-up-email).

## 3. Start the stack

```bash
docker compose up -d --build
```

On first start, Postgres creates the database and the API creates the schema with Flyway. Don't run
`seed-dev.sql` in production.

## 4. Deploy updates

```bash
git pull && docker compose up -d --build
```

## 5. Set up email

These steps use Resend over SMTP. Any other SMTP provider (Postmark, SES, and so on) works by changing
the `.env` values only.

1. Create a Resend account, add a sending domain, and add the DNS records it shows (SPF/MX and DKIM).
   Wait until the domain shows as "Verified".

2. Add a DMARC record for `_dmarc.yourdomain.com`:

   ```text
   TXT  v=DMARC1; p=none; rua=mailto:you@yourdomain.com
   ```

3. Create an API key with "Sending access" only.

4. Set these values in `.env`:

   ```env
   EMAIL_ENABLED=true
   MAIL_SMTP_HOST=smtp.resend.com
   MAIL_SMTP_PORT=587
   MAIL_SMTP_USERNAME=resend
   MAIL_SMTP_PASSWORD=<api key>
   MAIL_FROM=MeetingPlanner <no-reply@mail.yourdomain.com>
   APP_BASE_URL=https://yourdomain.com
   ```

   If your host blocks outbound port 587, use `2587`.

5. Check delivery in the outbox:

   ```sql
   SELECT status, attempts, last_error FROM email_outbox ORDER BY created_at DESC LIMIT 5;
   ```

   Failures are also logged as `event=email_send_failed`.

## 6. Back up the database

Back up Postgres nightly and copy the dumps off the server. For example, from cron:

```bash
docker exec postgres_db pg_dump -U postgres -Fc meeting_planner_db > /backups/mp-$(date +%F).dump
```

In a crontab, `%` must be escaped as `\%`.
