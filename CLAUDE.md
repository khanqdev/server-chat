# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project overview

A Spring Boot 4.1.1 / Java 25 chat server: REST + OTP-based registration/login (local and Google
Sign-In) backed by MongoDB, JWT access tokens + rotating refresh tokens, and a STOMP-over-WebSocket
layer (SockJS) for real-time 1:1 chat, group chat, typing indicators, and WebRTC call signaling.
It's a learning project (`com.example.chat_server`); code comments are written in Vietnamese and
error messages returned to clients are Vietnamese user-facing text — keep new comments/messages in
that style for consistency.

Note: the original package name `com.example.chat-server` is invalid Java, so the project uses
`com.example.chat_server` everywhere instead (see `HELP.md`).

## Commands

Build/run via the Maven wrapper (no local Maven install needed):

```
./mvnw compile              # compile
./mvnw spring-boot:run       # run the app (needs .env, see below)
./mvnw test                  # run all tests
./mvnw test -Dtest=ChatServerApplicationTests          # run a single test class
./mvnw test -Dtest=ChatServerApplicationTests#contextLoads   # run a single test method
./mvnw package                # build the jar
```

On Windows use `mvnw.cmd` instead of `./mvnw` from `cmd.exe`/PowerShell if `./mvnw` doesn't resolve.

### Local environment

Configuration is environment-driven via `spring.config.import=optional:file:.env[.properties]` in
`application.properties`. Copy `.env.example` to `.env` and fill in real values before running:

- `MONGODB_URI` — MongoDB Atlas (or local) connection string.
- `JWT_SECRET` — base64, ≥256 bits (`openssl rand -base64 64`).
- `GOOGLE_CLIENT_ID` — required only for Google Sign-In (`/api/auth/google`); that endpoint returns
  503 if unset.
- `MAIL_HOST`/`MAIL_*` — SMTP for OTP emails; if `MAIL_HOST` is blank, OTPs are only logged (dev-only
  fallback via `OtpMailService`).
- `RATE_LIMIT_*` — see Rate limiting below. **`RATE_LIMIT_SKIP_LOCALHOST` must be `false` in
  production.**

## Architecture

### Auth flow (two-step registration + JWT/refresh-token pair)

- **Registration is two-step and OTP-gated**: `AuthController` → `RegistrationService`.
  `POST /api/auth/register` creates a `PendingRegistration` (TTL-indexed Mongo doc, not a `User`
  yet) and emails a 6-digit OTP. `POST /api/auth/register/verify` atomically consumes an attempt via
  `MongoTemplate.findAndModify` (capped at 5 attempts, 5 min TTL) before comparing the OTP hash, then
  does an atomic `remove` on the pending doc so only one concurrent request can win and create the
  `User`. Resend has a 60s cooldown enforced server-side.
- **Local login** (`AuthService`) and **Google login** (`GoogleAuthService`, via
  `POST /api/auth/google`) both terminate in `TokenService.issueTokens`, which is the single place
  access + refresh tokens are minted.
- **Refresh tokens are single-use/rotating**: `TokenService.refresh` deletes the stored hash
  (`deleteByTokenHash` return value guards against concurrent double-use) before issuing a new pair.
  Only the SHA-256 hash of the refresh token is persisted (`RefreshTokenRepository`), never the raw
  token.
- **Google ID tokens** are verified with a `NimbusJwtDecoder` against Google's live JWKS, with
  explicit issuer + audience (`GOOGLE_CLIENT_ID`) validators — audience check matters because
  without it any token issued to _any_ Google app would validate. An existing local account can be
  linked to Google by matching verified email; an account can only ever link to one Google ID.
  Username is auto-generated from the email local-part when no account exists yet.
- JWTs are HS256, carry only the username as subject (`JwtTokenProvider`), and are stateless —
  there's no access-token revocation; only refresh tokens are revocable (`/api/auth/logout`).

### WebSocket / STOMP chat

- `WebSocketConfig` registers a single SockJS endpoint `/ws-chat`, brokers on `/topic` (public/group)
  and `/queue` (user-targeted), and app-prefixes inbound messages with `/app`.
- Auth for WebSocket is **not** handled by Spring Security's HTTP filter chain (which `permitAll`s
  `/ws-chat/**` — see `SecurityConfig`) but by `JwtChannelInterceptor`, a STOMP `ChannelInterceptor`:
  it requires a `Bearer` JWT on the STOMP `CONNECT` frame, validates it, and sets the resulting
  `Principal` on the session; subsequent `SEND`/`SUBSCRIBE` frames are rejected if no principal was
  set at CONNECT. When touching WebSocket auth, this interceptor — not `SecurityConfig` — is the
  place to look.
- `ChatController` handles all STOMP destinations (`/chat.sendMessage`, `/chat.addUser`,
  `/chat.private`, `/chat.group`, `/chat.typing`, `/chat.signal` for WebRTC offer/answer/ICE/etc.).
  **Sender identity and timestamp always come from the authenticated `Principal`, never from the
  client payload** (`withTrustedSender`) — don't trust `ChatMessage.sender` from the wire when adding
  new message types. Private/group chat messages are persisted to `MessageRepository`; typing and
  signaling messages are not persisted, just relayed.
- `WebSocketEventListener` handles connect/disconnect lifecycle (e.g. broadcasting leave events).

### Rate limiting

`RateLimitFilter` (registered via `RateLimitConfig` at `Ordered.HIGHEST_PRECEDENCE`, ahead of the
Spring Security chain) is a token-bucket limiter (Bucket4j) keyed by client IP, with per-IP buckets
cached in a bounded Caffeine cache (`RATE_LIMIT_MAX_TRACKED_CLIENTS`, 2h expire-after-access, to
bound memory under a flood of spoofed/rotating IPs). Every request consumes from a `general` bucket;
POSTs to specific auth endpoints (`login`/`google` share one budget, `register`/`resend-otp` share
another, `verify` has its own) additionally consume from an endpoint-specific bucket — both must
succeed. `/ws-chat/**` is exempt (SockJS HTTP-polling fallback legitimately makes many requests per
logical connection). `RATE_LIMIT_SKIP_LOCALHOST=true` (the dev default) bypasses limits entirely for
loopback IPs.

### Error handling

All exceptions funnel through `GlobalExceptionHandler` (`@RestControllerAdvice`) into a single
`ErrorResponse` shape (`status`, `error`, `fieldErrors`, `timestamp`). Business-logic failures should
be thrown as `ApiException(HttpStatus, message)` rather than ad-hoc exceptions, so they map to the
correct status and a user-facing Vietnamese message automatically. Bean Validation failures,
unreadable JSON bodies, duplicate-key races (two requests both passing an "already exists" check
before the unique index catches it), and mail-send failures each have dedicated handlers.

### Data layer

MongoDB via Spring Data (`spring.data.mongodb.auto-index-creation=true` — required since unique
indexes on username/email and TTL indexes on OTP/refresh-token expiry are declared on the entities,
not created manually). Key collections/repositories: `User`, `PendingRegistration` (OTP flow,
TTL-expired), `RefreshToken` (hash only, TTL-expired), `ChatMessage` ("messages").

## Stack versions

- Java 25, Spring Boot 4.1.1, Maven wrapper (`./mvnw`). Do not switch to Gradle or an older JDK.
- Frontend: React with TypeScript, shadcn/ui, Tailwind CSS, react-i18next (vi/en). Lives in `frontend/` or in the sibling FE repo.
- Realtime: STOMP. Current code exposes `/ws-chat` (SockJS). The agreed contract moves to `/ws` with `/user/queue/events` and `/user/queue/acks`. The contract is section 3 of `docs/source/05-api-contract.md`; feature designs in `docs/design/` refine it. Do not add new realtime features on the old endpoint.
- Note: this repo already contains some features beyond MVP1 (group chat, typing indicators, WebRTC signaling). Before building anything, check the existing code. Do not duplicate or rewrite existing features.

## Team workflow (agents in `.claude/agents/`)

This session is the **Lead**. Subagents cannot call other subagents, so the Lead coordinates them.

Agents: `product`, `architect`, `backend`, `frontend`, `qa`. Their prompts are in `.claude/agents/`.

### Stages and owner approval (mandatory)

1. **Requirements:** Lead calls `product` to write `docs/requirements/<feature>.md`. **Stop and wait for the owner to approve.**
2. **Design:** Lead calls `architect` to write `docs/design/<feature>.md` and ADRs in `docs/adr/`. **Stop and wait for the owner to approve.**
3. **Build:** Lead creates branch `feat/<feature>` from `main`. Then `backend` and/or `frontend` implement, each on that branch. Only start after step 2 is approved.
4. **Verify:** `qa` checks acceptance criteria, runs tests, and writes `docs/reviews/<feature>.md`. Fixes go back to `backend` or `frontend`.
5. **Merge:** Lead opens a PR with `gh pr create`. **Only the owner merges.** Agents never merge to `main`.

The Lead never moves to the next stage without approval. If an agent reports a blocker, the Lead reports it to the owner.

### Rules for all agents

- Write docs and comments in the style of this file: docs in English. Code comments and user-facing error messages stay in Vietnamese, as described above.
- Never read, print, or commit `.env` files, JWT secrets, Google client secrets, or MongoDB connection strings. Use placeholders in docs.
- Keep commits small with clear messages. Do not commit directly to `main`.
- Run `./mvnw test` (and frontend lint/tests if changed) before reporting success. Do not claim tests pass without running them.
- Respect rate-limit and auth design in this file. Do not weaken `JwtChannelInterceptor`, sender trust rules in `ChatController`, or refresh-token rotation.

### Repo layout for agents

- Backend: `src/main/java/com/example/chat_server/` (Spring Boot), tests in `src/test/`.
- Docs: `docs/source/` (planning docs, read-only input, see below), `docs/requirements/`, `docs/design/`, `docs/adr/`, `docs/reviews/`.
- Keep the Vietnamese error-message convention when adding new `ApiException` messages.

## Source docs (`docs/source/`)

Planning docs written in Vietnamese with the owner. They are the input for every agent. Agents read them but do not edit them; if one is wrong or out of date, report it to the Lead.

| File | What it holds | Main readers |
|------|---------------|--------------|
| `01-phan-tich-nghiep-vu.md` | Business analysis: modules M1–M16, business rules, priorities | product |
| `02-user-stories-mvp.md` | User stories with story IDs and acceptance criteria (v0.2) | product, qa |
| `03-roadmap-mvp-moi.md` | Roadmap v0.4: "MVP gọn" scope, sprints, milestones | product, Lead |
| `04-kien-truc-ky-thuat.md` | Technical architecture and tech stack (v0.1) | architect, backend |
| `05-api-contract.md` | API contract v0.2: DTOs, REST `/api/v1`, STOMP `/ws`, limits, error codes | architect, backend, frontend, qa |

If the contract and the current code disagree, the contract wins for new work. Existing endpoints already aligned with it are the auth endpoints (contract section 2.1).

## MVP scope and status (source: `docs/source/03-roadmap-mvp-moi.md`)

- Scope: "MVP gọn", 120 story points. Target launch 28–31/01/2027, before Tết (06/02/2027). Feature freeze 17/01/2027.
- Users: about 100 people, email and Google sign-in only. No phone sign-in in MVP.
- Current status (19/11/2026):
  - Done: M1-01, M1-02, M1-03, M1-04, M1-06, M1-08 (email, OTP, login, password reset, logout, Google, both FE and BE).
  - In progress: M1-05 (multi-device sessions work; message sync waits for chat), M16-02 (i18n and language switch exist; full translation pending).
  - Not started: M2 (profile, search, friends), M3 (1-1 chat), M4 (groups), M5 (files), M6 (notifications), M7 (search).
- The repo already has group chat, typing, and WebRTC signaling code from earlier work. This code is a test version and does not match the contract. Do not extend WebRTC or calls. Calls are phase 2.
- Next sprint (S4, 16/11–29/11): M2-01..05 plus the realtime setup described above.
- Demo 1 is 13/12/2026: two users chat 1-1 in realtime. If it is not running by then, the Lead cuts scope in this order: M5-03, M6-03, M4-06, M7-01.
- Definition of done: tests pass, acceptance criteria met, code reviewed by a human (the owner or a teammate), deployed to staging. An agent's review (qa) does not replace the human review.
