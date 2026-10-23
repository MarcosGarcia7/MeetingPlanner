# MeetingPlanner API (server)

Spring Boot 4 / Java 21 / Maven. Postgres via JPA + Flyway, Redis, Spring Security OAuth2 resource
server (HS256 JWTs), Spring Mail.

## Commands

Run from `server/` (no Maven wrapper — use a local `mvn`):

```bash
mvn test                                   # all tests
mvn test -Dtest=GroupServiceImplTest       # one class
mvn test -Dtest=GroupServiceImplTest#create*  # matching methods
mvn spring-boot:run                        # needs Postgres + Redis + env (see server/.env.example)
mvn -q -B package -DskipTests              # what the Dockerfile does
```

## Package layout

`com.meetingplanner`:

- `domains/<domain>/{controller,model,repository,service}` — domains: `user`, `friend`, `group`,
  `meeting`, `invite`, `request` (join requests), `notification`, `email`, `search`.
- `common/api/dto` — **all** request/response records live here, not in the domain packages.
- `common/api/exceptions` — domain exceptions + `ApiExceptionHandler`.
- `common/api` — shared helpers: `CursorCodec`, `SearchCursorCodec`, `SearchQuery`, `Timezones`.
- `common/security` — `CurrentUser`, `InviteTokens`.
- `config/` — security, Redis, mail.

## Conventions

- **Controllers** are thin: take `@AuthenticationPrincipal Jwt jwt`, call
  `CurrentUser.from(jwt).id()`, delegate to a service. Use `@Valid @RequestBody`. Return
  `ResponseEntity.noContent()` for void actions and `HttpStatus.CREATED` for creates of child resources.
- **Services** take `long userId` (the acting user) as the first argument. Larger domains use an
  interface + `*Impl` (`GroupService`/`GroupServiceImpl`); match whatever the domain already does.
  Constructor injection only, `final` fields, no Lombok.
- Mark service methods `@Transactional` / `@Transactional(readOnly = true)` explicitly.
- **Authorization** belongs in the per-domain `*AuthorizationService` (`GroupAuthorizationService`,
  `MeetingAuthorizationService`, `UserProfileAuthorizationService`), not inline role checks.
- **Blocking**: any interaction between two users (invites, friend requests, join requests, adding to
  groups/meetings) must check `BlockService.assertNotBlocked(...)`. Blocking also invalidates pending
  items between the pair (`invalidatePendingBetween`).
- **Errors**: throw `ResourceNotFoundException`, `ForbiddenException`, or a specific exception from
  `common/api/exceptions`; use `ResponseStatusException` for one-off 4xx cases. Every error body is
  `{ "error": "<snake_case_code>", "message": "..." }`. A new exception type needs a handler in
  `ApiExceptionHandler` with a stable `error` code — the BFF branches on those codes.
- Unique-constraint races: check `existsBy…` first, then also catch `DataIntegrityViolationException`
  on save and map it to the same domain exception.
- **Pagination** is cursor-based: return `CursorPage<T>(items, nextCursor)`; use
  `CursorCodec.clampSize(size)` (default 20, max 100), fetch `limit + 1` rows to detect a next page,
  and encode cursors with `CursorCodec`. Search endpoints use `SearchQuery` + `SearchCursorCodec`.
- Times are `OffsetDateTime`, stored UTC. User timezones go through `Timezones.resolve`.
- **Notifications / email**: go through `NotificationService` (`push`, `queueEmailTo…`). Email is an
  outbox: rows in `email_outbox` are drained by `MailDeliveryWorker`; templates are in
  `EmailTemplateRegistry`. Never send mail synchronously from a request.
- Background jobs are `@Scheduled` `*Worker` components in the owning domain's `service` package, with
  intervals configurable via `app.*` properties in `application.properties`.

## Database

- Schema is a single Flyway migration, `src/main/resources/db/migration/V1__init.sql`. The app is
  not deployed yet and the dev DB holds little data, so schema changes are made **by editing V1 in
  place**. The user resets the database themselves afterwards. Don't run destructive DB commands;
  just say a reset is needed. Confirm with the user before switching to adding `V2__…` files.
- `ddl-auto=validate`: every entity change needs a matching SQL change or the app won't start.
- Extensions in use: `pgcrypto`, `citext` (case-insensitive emails/usernames).
- Composite keys use `@EmbeddedId` classes named `<Thing>Id` (e.g. `GroupMemberId`, `GroupBanId`).

## Security

- Public paths: `/api/auth/**` and `/api/invite-links/**`; everything else requires a valid access JWT.
- Separate access and refresh secrets/encoders/decoders; inject the right one with
  `@Qualifier("accessJwtDecoder")` etc. Refresh tokens rotate; reuse revokes all sessions (`JtiStorageService`).
- Login lockout and rate limits are Redis-backed (`LoginAttemptService`, `AccountLockoutService`).
  The client IP arrives via `X-Forwarded-For` set by the BFF.
- 401/403 from the filter chain are rendered as JSON by `JsonAuthenticationEntryPoint` /
  `JsonAccessDeniedHandler`.

## Tests

- Plain JUnit 5 + Mockito unit tests (`@ExtendWith(MockitoExtension.class)`, `@Mock` repositories,
  `@InjectMocks` service). No Spring context or database is spun up — keep it that way unless asked.
- Tests mirror the main package path (`domains/group/service/GroupServiceImplTest`).
- When changing a service, add/adjust tests for the forbidden, not-found, and blocked paths, not just
  the happy path.
