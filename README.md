# MeetingPlanner

A web app for scheduling meetings with friends and groups. It warns you about scheduling conflicts
before you commit to a time, and lets participants vote on proposed time changes.

## Features

- Friends and groups, with invites and join requests
- Meeting scheduling with conflict warnings
- Voting on time changes
- In-app notifications and email

## Tech stack

- **API:** Spring Boot 4, Java 21, PostgreSQL 16, Flyway
- **Frontend and BFF:** Next.js 16, React 19, TypeScript
- **Sessions and rate limiting:** Redis
- **TLS proxy:** Caddy
- **Local and production runtime:** Docker Compose

## Architecture

```text
Browser → Caddy (HTTPS) → BFF (Next.js, :3000) → API (Spring Boot, :8080) → PostgreSQL
                               ↓                    ↓
                             Redis                Redis
```

The browser only talks to the BFF and never sees a JWT. The BFF keeps access and refresh tokens in a
Redis session, identified by an httpOnly `mp_session` cookie, and calls the API with a bearer token.
The API is stateless and only reachable on the internal Docker network.

## Getting started

### Prerequisites

- Docker with Docker Compose
- [mkcert](https://github.com/FiloSottile/mkcert), for local HTTPS

### Setup

1. Copy `.env.example` to `.env`. The defaults are set up for `https://localhost`.

2. Generate a local TLS certificate from the repo root. You only need to do this once:

   ```bash
   mkcert -install
   mkdir certs
   mkcert -cert-file certs/localhost.pem -key-file certs/localhost-key.pem localhost 127.0.0.1 ::1
   ```

3. Start the stack:

   ```bash
   docker compose up --build
   ```

   This starts Postgres, Redis, the API, the BFF, and the proxy. The API applies database migrations
   when it starts.

4. Open https://localhost.

### Seed data (optional)

Once the stack is running, load the development fixtures:

```bash
docker cp seed-dev.sql postgres_db:/tmp/seed-dev.sql
docker exec postgres_db psql -U postgres -d meeting_planner_db -f /tmp/seed-dev.sql
```

This creates the following accounts, all with the password `Password123!`:

| Account                                      | Verified | Seeded data                                 |
| -------------------------------------------- | -------- | ------------------------------------------- |
| `alice@example.com`                          | yes      | Friends, friend requests, groups, meetings  |
| `bob@example.com`                            | yes      | Friendship with `alice`, a group, meetings  |
| `unverified@example.com`                     | no       | None                                        |
| `user01@example.com` to `user60@example.com` | yes      | Connections to `alice` and her groups       |

Sign in at https://localhost/login with an email or username. Most of the data is built around `alice`
to exercise pagination:

- **Friends:** 36 accepted friends and 25 pending incoming requests
- **Groups:** membership in 25 groups, 10 of which `alice` owns
- **Group members:** `Seed Group 01` has 61 members, and `bob` is a member of `Seed Group 03`
- **Private data:** every 10th bulk user and every even-numbered group are private

`alice` and `bob` also share a small meeting scenario, with dates relative to when you first run the
script:

| Meeting               | Creator | Participants              | When          | Notes                    |
| --------------------- | ------- | ------------------------- | ------------- | ------------------------ |
| `Sprint Retro`        | `alice` | `alice`, `bob`            | 3 days ago    | In `Seed Group 03`       |
| `Project Kickoff`     | `alice` | `alice`, `bob`            | In 2 days     |                          |
| `Design Review`       | `bob`   | `alice`, `bob`, `user01`  | In 3 days     | Has a pending time change |
| `Team Sync`           | `alice` | `alice`, `bob`            | In 4 days     | In `Seed Group 03`       |
| `Dentist Appointment` | `alice` | `alice`                   | In 5 days     | Private                  |
| `Client Call`         | `bob`   | `bob`                     | In 6 days     |                          |

To try the main workflows:

- **Time change voting:** `bob` proposed moving `Design Review` and `user01` has approved. Sign in as
  `alice` to approve or decline it, or as `bob` to cancel it. The proposal expires after 48 hours, and
  re-running the script creates a new one.
- **Conflict warnings:** the proposed time overlaps `Dentist Appointment`, so `alice` sees a conflict
  when voting. She also gets one when scheduling over her own meetings or inviting `bob` to a time that
  overlaps `Client Call`.

Use `unverified` to test the email verification prompt and resend link.

`seed-dev.sql` is for development only. Don't run it in production or move it into `db/migration`.

## Development

### Running the apps on the host

You'll need Java 21, Maven, and Node.js. `node_modules/` isn't needed if you only run the app in
Docker.

Start the infrastructure from the repo root. Postgres is published on port 5434 and Redis on 6379:

```bash
docker compose up db redis
```

Run the API from `server/`, after copying `server/.env.example` to `server/.env`:

```bash
mvn spring-boot:run
```

Run the BFF from `bff/`, after copying `bff/.env.example` to `bff/.env`:

```bash
npm install
npm run dev
```

Then open http://localhost:3000.

### Tests and checks

From `server/`:

```bash
mvn test            # unit tests
mvn verify -Pit     # unit tests + Testcontainers integration tests (needs Docker)
```

From `bff/` (there's no test suite yet):

```bash
npm run lint        # ESLint and Stylelint
npx tsc --noEmit    # typecheck
```

### Reading emails locally

With `EMAIL_ENABLED=false`, no mail is sent, but messages are still queued in the `email_outbox` table.
To get the latest verification link, open a `psql` session:

```bash
docker exec -it postgres_db psql -U postgres -d meeting_planner_db
```

Then run:

```sql
SELECT payload->>'link' FROM email_outbox WHERE template = 'EMAIL_VERIFY' ORDER BY created_at DESC LIMIT 1;
```

The link is built from `APP_BASE_URL`, so check that it matches the address you're using.

## Troubleshooting

### `database "meeting_planner_db" does not exist`

The `pgdata` volume was first initialized with a different `DB_NAME`. Postgres only creates the
database on an empty volume, so you'll need to do one of the following:

- Create the database:

  ```bash
  docker exec -it postgres_db psql -U postgres -c "CREATE DATABASE meeting_planner_db;"
  ```

- Reset the volume, which **deletes all data**:

  ```bash
  docker compose down -v
  ```

## Deployment

See [docs/DEPLOYMENT.md](docs/DEPLOYMENT.md) for VPS setup, production email, and backups.

## Documentation

- [API_ENDPOINTS.md](API_ENDPOINTS.md): API endpoint inventory
- [.env.example](.env.example): Docker Compose configuration
- [server/.env.example](server/.env.example) and [bff/.env.example](bff/.env.example): configuration
  for running on the host
