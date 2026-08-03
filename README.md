# 🐟 FreshLink — B2B Fish Supply Backend

> A Spring Boot REST API connecting fish suppliers with cafés and restaurants in Kandy, Sri Lanka — replacing informal phone-and-WhatsApp ordering with a structured marketplace and a tracked order lifecycle.

<p align="left">
  <img src="https://img.shields.io/badge/Java-21-ED8B00?style=flat-square&logo=openjdk&logoColor=white" alt="Java 21"/>
  <img src="https://img.shields.io/badge/Spring%20Boot-4.0.1-6DB33F?style=flat-square&logo=springboot&logoColor=white" alt="Spring Boot"/>
  <img src="https://img.shields.io/badge/PostgreSQL-316192?style=flat-square&logo=postgresql&logoColor=white" alt="PostgreSQL"/>
  <img src="https://img.shields.io/badge/Flyway-CC0200?style=flat-square&logo=flyway&logoColor=white" alt="Flyway"/>
  <img src="https://img.shields.io/badge/Spring%20Security-JWT-6DB33F?style=flat-square&logo=springsecurity&logoColor=white" alt="Spring Security"/>
  <img src="https://img.shields.io/badge/Swagger-85EA2D?style=flat-square&logo=swagger&logoColor=black" alt="Swagger"/>
  <img src="https://img.shields.io/badge/Docker-2496ED?style=flat-square&logo=docker&logoColor=white" alt="Docker"/>
  <img src="https://img.shields.io/badge/tests-93%20passing-brightgreen?style=flat-square" alt="93 tests"/>
</p>

---

## Contents

- [The Problem](#the-problem) · [Features](#features) · [Architecture](#architecture) · [Data Model](#data-model)
- [API Reference](#api-reference) · [Order Lifecycle](#order-lifecycle) · [Delivery](#delivery) · [Demand Matching](#demand-matching)
- [Getting Started](#getting-started) · [API Documentation](#api-documentation) · [Running with Docker](#running-with-docker)
- [Security](#security) · [Testing](#testing) · [Continuous Integration](#continuous-integration)
- [Configuration](#configuration) · [Database Migrations](#database-migrations) · [Error Responses](#error-responses)
- [Tech Stack](#tech-stack) · [Roadmap](#roadmap) · [Author](#author)

---

## The Problem

Small café owners in Kandy source fish through informal daily phone calls. There's no price visibility, no record of what was ordered, and no accountability when a delivery falls short. Suppliers, in turn, have no reliable read on demand before they buy at the market.

FreshLink puts that exchange behind an API: suppliers publish what they have, cafés browse and order, and every order carries a status that both sides can see.

It runs two complementary trade flows:

- **Spot market** — suppliers list available catch, cafés browse and order it directly.
- **Demand matching** — cafés post forward demand for a future date, and a scheduled engine pairs it against suppliers' daily catch, ranked by freshness and supplier rating.

---

## Features

**Trading**

- **Spot marketplace** — suppliers publish listings; cafés browse by species and city, paged and sorted. Suspended and removed suppliers never appear.
- **Tracked order lifecycle** — accept, reject, delivering and completion transitions, each a distinct authorised endpoint.
- **Concurrency-safe stock** — reservations are guarded by an optimistic-locking version column, proven by concurrent integration tests: without it, two 60 kg orders against 100 kg of stock both succeed.
- **Demand matching engine** — greedy allocation ranked by freshness, then supplier rating, then catch time. Re-runs every 10 minutes, allocates only the outstanding shortfall, and expires matches a supplier never answers so their supply returns to the pool.
- **Delivery routes** — one driver, one trip, several drop-offs. Dispatching stamps the driver on every stop and puts them all on the road in one call.
- **Delivery tracking** — driver, phone, ETA and arrival time per order, with validated status transitions. Cafés can see where their fish is.
- **Price history and market analytics** — every price a listing has charged, plus the market-wide spread and daily trend per fish type, so cafés can judge an offer and suppliers can price against the market.
- **Post-delivery ratings** — cafés rate suppliers once an order completes, feeding both the leaderboard and the matching engine's ranking.

**Platform**

- **Three-role access model** — `SUPPLIER`, `CAFE`, `ADMIN`, enforced with method-level `@PreAuthorize` on every controller.
- **Ownership enforced in the service layer** — below the controller, so it cannot be bypassed by a new caller.
- **JWT auth with rotating refresh tokens** — hashed at rest, rotated on every use, replay treated as theft.
- **Rate-limited auth endpoints** — token-bucket caps per IP and per email address, in-process or shared through Redis.
- **Email OTP verification** — 6-digit codes, 5-minute expiry, capped attempts.
- **Transactional notifications** — sent after commit on a background pool, so mail never delays or fails a request.
- **Admin oversight** — account activation, soft deletion with safeguards, analytics dashboard.
- **Versioned schema** — Flyway owns every table; Hibernate only validates.
- **Interactive API docs** — Swagger UI with JWT authorisation wired in.
- **Container-ready** — multi-stage Dockerfile, Compose stack, health and readiness probes.

---

## Architecture

A conventional layered Spring Boot structure, with interfaces separating contracts from implementations:

```
Controller  →  Service (interface)  →  ServiceImpl  →  Repository  →  PostgreSQL
     ↓                                       ↓
    DTO   ←────────────  Mapper  ←────────  Entity
```

Entities never cross the controller boundary. Dedicated request/response DTOs and hand-written mappers keep the persistence model out of the API surface — which also avoids the bidirectional-relationship JSON recursion that plagues naive JPA REST layers.

```
src/main/java/com/freshlink/
├── controller/     # Auth, Cafe, Supplier, Demand, Admin, Analytics
├── service/
│   └── interfaces/ # Service contracts
│       └── impl/   # Implementations
├── Repository/     # Spring Data JPA repositories
├── model/          # JPA entities
├── security/       # SecurityConfig, JwtFilter, RateLimitFilter, UserPrincipal
├── notification/   # Domain events + after-commit email listener
├── exception/      # Typed exceptions + global handler
├── util/           # JwtUtil
├── enums/          # Role, OrderStatus, DemandStatus, MatchStatus, DeliveryStatus, ...
├── mapper/         # Entity ↔ DTO mapping
├── config/, cache/ # OpenAPI, async pool, caching, scheduled eviction
└── *dto/           # Request/response DTOs, grouped by domain
```

---

## Data Model

```
                    ┌──────────────┐
                    │  user_table  │  JOINED inheritance
                    └──────┬───────┘
              ┌────────────┼────────────┐
        ┌─────▼────┐  ┌────▼───┐  ┌─────▼──────┐
        │  admin   │  │  cafe  │  │  supplier  │
        └──────────┘  └───┬────┘  └─────┬──────┘
                          │             │
              ┌───────────┤             ├──────────────┐
              │           │             │              │
      ┌───────▼──────┐ ┌──▼─────┐  ┌────▼────┐  ┌──────▼───────┐
      │demand_request│ │ orders │  │  fish   │  │ daily_supply │
      └───────┬──────┘ └──┬──┬──┘  └────┬────┘  └──────┬───────┘
              │           │  │          │              │
              │      ┌────▼┐ │   ┌──────▼───────────┐  │
              │      │items│ │   │fish_price_history│  │
              │      └─────┘ │   └──────────────────┘  │
              │              │                         │
              │        ┌─────▼────┐                    │
              │        │ delivery │──▶ delivery_route   │
              │        └──────────┘                    │
              │                                        │
              └──────────► supply_match ◄──────────────┘
                                │
                                └──▶ orders.supply_match_id
```

Users use **JOINED inheritance**: one row in `user_table` per account plus one in the
subtype table sharing its id. An accepted `supply_match` creates an `order` and links
back to itself, so a matched order can be traced to the demand that produced it — spot
orders leave that link null.

---

## API Reference

Base path: `/api/v1`. Everything except `/auth/**` requires `Authorization: Bearer <token>`.

### Authentication — `/auth`

| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `POST` | `/auth/register/cafe` | Register a café account |
| `POST` | `/auth/register/supplier` | Register a supplier account |
| `POST` | `/auth/request-otp` | Send a verification OTP to an email |
| `POST` | `/auth/verify-otp` | Confirm the OTP and verify the account |
| `POST` | `/auth/login` | Authenticate, receive access + refresh tokens |
| `POST` | `/auth/refresh` | Exchange a refresh token for a new access token **and a new refresh token** — store both, the old one stops working |
| `POST` | `/auth/logout` | Invalidate the refresh token |

New accounts are created inactive and require **both** email verification and admin activation before login succeeds.

### Supplier — `/suppliers` · role `SUPPLIER`

| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `GET` | `/suppliers/me` | Current supplier profile |
| `POST` | `/suppliers/fish` | Add a fish listing |
| `GET` | `/suppliers/fish` | List own listings (paged) |
| `PUT` | `/suppliers/fish/{id}` | Update a listing |
| `DELETE` | `/suppliers/fish/{id}` | Remove a listing |
| `GET` | `/suppliers/fish/{id}/price-history` | What this listing has charged over time |
| `GET` | `/suppliers/orders` | Incoming orders (paged) — optional `?status=REQUESTED` |
| `PUT` | `/suppliers/orders/{orderId}/accept` | Accept an order |
| `PUT` | `/suppliers/orders/{orderId}/reject` | Reject an order |
| `PUT` | `/suppliers/orders/{orderId}/markdelivering` | Mark as out for delivery |
| `PUT` | `/suppliers/orders/{orderId}/complete` | Mark as delivered — also settles the delivery |
| `GET` | `/suppliers/orders/{orderId}/delivery` | Delivery detail |
| `PUT` | `/suppliers/orders/{orderId}/delivery` | Assign driver, set ETA, advance status |
| `POST` | `/suppliers/routes` | Plan a route — group several delivering orders into one trip |
| `GET` | `/suppliers/routes` | Own routes (paged) |
| `GET` | `/suppliers/routes/{routeId}` | Route with its stops |
| `PUT` | `/suppliers/routes/{routeId}/status` | Dispatch, complete or cancel the whole route |
| `PUT` | `/suppliers/routes/{routeId}/stops/{orderId}` | Add a stop while still planning |
| `DELETE` | `/suppliers/routes/{routeId}/stops/{orderId}` | Take a stop off — the delivery survives |
| `DELETE` | `/suppliers/routes/{routeId}` | Delete while still planned — unassigns stops, never deletes them |
| `POST` | `/suppliers/daily-supply` | Record today's catch — triggers matching immediately |
| `GET` | `/suppliers/daily-supply` | List own recorded catch (paged) |
| `PUT` | `/suppliers/daily-supply/{id}` | Adjust quantity or freshness |
| `DELETE` | `/suppliers/daily-supply/{id}` | Remove an unmatched entry |
| `GET` | `/suppliers/supply-matches` | Pending demand matches |
| `PUT` | `/suppliers/supply-matches/{id}/accept` | Accept a match — deducts supply, creates an order |
| `PUT` | `/suppliers/supply-matches/{id}/reject` | Reject a match — returns demand to the pool |

### Café — `/cafes` · role `CAFE`

| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `GET` | `/cafes/me` | Current café profile |
| `GET` | `/cafes/market/fish` | Browse the marketplace (paged) — optional `fishType`, `city` filters |
| `GET` | `/cafes/market/fish/{fishId}/price-history` | Price trend for a listing — judge whether today's offer is fair |
| `POST` | `/cafes/orders` | Place an order (single supplier per order) |
| `GET` | `/cafes/orders` | Order history (paged) — optional `?status=DELIVERING` |
| `PUT` | `/cafes/orders/{orderId}/cancel` | Cancel while still pending |
| `GET` | `/cafes/orders/{orderId}/delivery` | Track the delivery — driver, ETA, status |
| `POST` | `/cafes/orders/{orderId}/rate` | Rate the supplier after completion, 1–5 |
| `GET` | `/cafes/ratings` | Ratings this café has left (paged) |

### Market — `/market` · roles `CAFE` and `SUPPLIER`

Open to both trading sides: the same numbers answer *is this offer fair* for a café
and *am I priced competitively* for a supplier.

| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `GET` | `/market/price-summary` | Lowest, highest and average price per fish type across every live listing, with listing count, total available, and whether it is in season |
| `GET` | `/market/fish-types/{fishTypeId}/price-trend?days=30` | Daily market average over the window. A day with no point means nobody changed their price. |

### Demand — `/demand` · role `CAFE`

| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `POST` | `/demand/create` | Post a forward demand request |
| `GET` | `/demand` | List own demand (paged) |
| `DELETE` | `/demand/{demandId}/delete` | Withdraw a demand request |

### Admin — `/admin` · role `ADMIN`

| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `GET` | `/admin/suppliers` | List all suppliers (paged) |
| `GET` | `/admin/cafes` | List all cafés (paged) |
| `PUT` | `/admin/users/{id}/activate` | Activate a user account |
| `DELETE` | `/admin/users/{id}/delete` | Soft-delete an account |
| `GET` | `/admin/analytics/dashboard` | Revenue, order and demand metrics |
| `GET` | `/admin/analytics/suppliers` | Supplier performance leaderboard |

### Paging

List endpoints take `page`, `size` and `sort` and return a Spring `Page`:

```
GET /api/v1/cafes/market/fish?page=0&size=20&sort=pricePerKg,asc
```

```json
{ "content": [ ... ], "totalElements": 57, "totalPages": 3, "number": 0, "size": 20 }
```

Default size is 20, capped at 100.

---

## Order Lifecycle

```
   Café places order
          │
          ▼
    [ REQUESTED ] ──── café cancels ────▶ [ CANCELLED ]
          │
     supplier decides
       ┌──┴──┐
       ▼     ▼
 [ACCEPTED] [REJECTED]
       │
  markdelivering
       ▼
  [ DELIVERING ]
       │
    complete
       ▼
  [ COMPLETED ] ────▶ café submits rating
```

Stock is reserved the moment an order is placed. Rejection and cancellation both return it.

## Delivery

A delivery record is raised when the supplier marks an order as delivering.

```
SCHEDULED ──▶ IN_TRANSIT ──▶ DELIVERED
    │              │
    └──────▶ FAILED ◀┘
               │
               └──▶ IN_TRANSIT   (retry)
```

`DELIVERED` is terminal. The arrival time is stamped by the server rather than
supplied by the caller, so a late delivery cannot be backdated. Completing the order
settles the delivery too, so the two can never drift apart.

### Routes

A supplier's van goes out with several cafés on board, so deliveries group into a route.

```
PLANNED ──▶ DISPATCHED ──▶ COMPLETED
   │             │
   └──▶ CANCELLED ◀┘
```

Dispatching moves every `SCHEDULED` stop to `IN_TRANSIT` and stamps the driver on each,
instead of retyping the same details per delivery. Completing requires every stop to be
delivered or failed — a failed drop counts as resolved, so one bad address cannot block
the van. Cancelling **detaches** the stops rather than deleting them: those deliveries
still have to happen, just on another van.

---

## Demand Matching

```
Supplier records daily catch      Café posts demand
          │                              │
          └──────────► matching ◄────────┘
                          │
             ranked by freshness score,
             then rating, then catch time
                          │
                    [ SupplyMatch ]
                     ┌────┴────┐
                accept        reject
                   │             │
            deducts supply,   demand returns
            creates order      to the pool
```

Matching runs on creation and again every 10 minutes for demand still open or partially
filled, and only ever allocates the outstanding shortfall.

**One ledger.** `Fish.availableKg` is the single source of truth for sellable stock; a
`DailySupply` row is the dated intake behind it, carrying the freshness the matcher ranks
on. Recording a catch credits the listing, correcting one moves both together, and
removing one takes its quantity back off — refused if that fish has already been sold.
They were previously independent numbers, so the spot market and the matching engine
could each believe they held stock the other had sold.

A pending match reserves part of a supply, so one a supplier never answers is expired
after 24 hours (`MATCH_PENDING_TIMEOUT_HOURS`) and its quantity returns to the pool.
Demand whose delivery date has passed is closed rather than retried forever.

---

## Getting Started

### Prerequisites

- JDK 21+
- PostgreSQL 14+
- An SMTP account, only if you want OTP and notification email to actually send

### Setup

```bash
git clone https://github.com/sudeshkar/FreshLink-Backend.git
cd FreshLink-Backend
```

Create the database — Flyway creates every table, so do not create them by hand:

```sql
CREATE DATABASE "FL_db";
```

Copy the local configuration template and fill in your own values:

```bash
cp src/main/resources/application-local.properties.example \
   src/main/resources/application-local.properties
```

`application-local.properties` is git-ignored and is the **only** place local credentials belong. It overrides whatever the active profile sets.

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/FL_db
spring.datasource.username=YOUR_DB_USER
spring.datasource.password=YOUR_DB_PASSWORD

# Optional — needed only if you want OTP and notification emails to send
spring.mail.username=YOUR_EMAIL
spring.mail.password=YOUR_APP_PASSWORD
```

> If PostgreSQL was installed alongside an existing instance it may be listening on **5433** rather than 5432. Check with `pg_isready -p 5433`.

Run it:

```bash
./mvnw spring-boot:run        # macOS / Linux
mvnw.cmd spring-boot:run      # Windows
```

The API comes up on `http://localhost:8080`.

On first start the application applies migrations, seeds fish-type reference data and demo traders, and creates the admin account from `app.admin.*` if both values are set. No credentials are compiled into the source — omit them and admin bootstrapping is skipped with a warning.

### Trying it out

```bash
# Register a café
curl -X POST http://localhost:8080/api/v1/auth/register/cafe \
  -H "Content-Type: application/json" \
  -d '{"email":"cafe@example.com","password":"secret123","name":"Kandy Coffee House"}'

# Verify with the OTP sent to that address, then log in
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"cafe@example.com","password":"secret123"}'

# Use the returned token
curl http://localhost:8080/api/v1/cafes/market/fish?city=Kandy \
  -H "Authorization: Bearer <ACCESS_TOKEN>"
```

---

## API Documentation

Swagger UI is served in development at:

```
http://localhost:8080/swagger-ui.html
```

The OpenAPI 3 schema is at `/v3/api-docs` — point your frontend's client generator at it.

To call protected endpoints from the UI: `POST /api/v1/auth/login`, copy the access token,
click **Authorize**, and paste it.

Both are disabled on the `prod` profile: a published schema is an attack map, so expose it
deliberately rather than by default.

---

## Running with Docker

```bash
export JWT_SECRET=at-least-32-characters-of-random-text-here
docker compose up --build
```

Brings up Postgres, Redis and the API together, with the Redis-backed limiter and cache
enabled so `docker compose up --scale app=3` behaves correctly rather than tripling every
rate limit. The app waits for the database to pass
its healthcheck before starting, so Flyway does not race it.

Compose runs the **prod** profile, which by design has no fallback values — every
variable in the [configuration table](#production-environment-variables) must be set or
startup fails loudly.

The image builds from the `maven` base rather than `./mvnw`: the wrapper is checked out
with CRLF line endings on Windows and a Linux container rejects it. It runs as a non-root
user, and `.dockerignore` keeps `application-local.properties` out of every layer.

### Health probes

| Endpoint | Purpose |
| :--- | :--- |
| `/actuator/health` | Overall status |
| `/actuator/health/readiness` | Ready to serve — includes a database check |
| `/actuator/health/liveness` | Process is alive |

Only `health` and `info` are exposed. `env`, `beans`, `configprops` and
`heapdump` stay off — they leak configuration and internals.

Mail is deliberately excluded from health: OTP delivery failing does not stop the
API serving traffic, and letting an SMTP blip mark the app DOWN invites an
orchestrator to restart a process that is working fine.

---

## Security

### Authentication

Short-lived JWT access tokens (5 minutes) with long-lived refresh tokens (7 days).
`JwtUtil` refuses to start if the signing secret is under 32 bytes — HS256 requires
256 bits, and a shorter key would be silently weak.

Refresh tokens are **256 bits of randomness, stored only as SHA-256 hashes**. A database
dump therefore yields no usable credentials. They **rotate on every use**: the response
carries a new token and the one presented stops working.

Spent tokens are retained rather than deleted so that replay is detectable. Presenting one
again revokes **every session** for that account — there is no way to tell the rightful
holder from whoever copied it, so both are signed out. Refreshing also re-checks that the
account is still active and not soft-deleted, so a suspended user cannot refresh
indefinitely.

### Authorisation

Role checks sit on the controller with `@PreAuthorize`. **Resource ownership is enforced
in the service layer**, below the controller, so a future caller cannot bypass it.

An ownership mismatch returns **404, not 403**. A 403 confirms the id exists and lets one
account enumerate another's orders, demand or listings.

### Rate limiting

Token buckets on the only endpoints reachable without a token:

| Endpoint | Default |
| :--- | :--- |
| `/auth/login` | 5 per IP per 15 min |
| `/auth/request-otp` | 3 per IP per 15 min, **plus** 3 per email address |
| `/auth/verify-otp` | 3 per IP per 15 min |

The limit applies to the endpoint rather than to failures, so exhausting it blocks a
correct password too — counting only failures would let an attacker reset their allowance
with one valid login. The per-email cap exists because an IP-only limit still lets a
rotating pool of addresses bury one inbox in codes.

Buckets are in-process by default. Set `RATELIMIT_BACKEND=redis` to share them across
instances — with per-process buckets, three replicas turn a five-attempt limit into fifteen.

### Account lifecycle

Accounts are **soft-deleted** — the row is retained so orders, ratings and analytics keep
their references and trade history stays intact. Deletion is refused when the account is
the last remaining admin, is the acting admin's own account, or still has orders in
progress.

Removed suppliers are excluded from the market, from matching, and from direct ordering by
fish id. Email addresses are deliberately **not** released, so a removed supplier cannot
re-register on the same address to shed its rating history.

### Revoking access

A signed token is valid until it expires, so suspending an account would otherwise leave it
working for the rest of that token's lifetime. Every authenticated request checks whether the
account is still usable, through a cache expiring after a minute — so revocation takes effect
in about a minute, at roughly one query per user per minute rather than one per request.

Set `CACHE_BACKEND=redis` to share that answer across instances; otherwise each replica keeps
its own copy and one can still accept a suspended account after another has noticed.

### Observability

Every request carries an `X-Request-Id`, generated if the caller does not supply one and
echoed back on the response. It appears in each log line for that request, and is propagated
onto the notification pool — so work that happens after the response, on another thread, is
still traceable to the request that caused it.

### Other measures

- Sessions are stateless; CSRF is disabled because there is no cookie-borne ambient authority to protect.
- CORS origins come from configuration, not a wildcard.
- Passwords are BCrypt-hashed.
- Production hides stack traces, error messages and health detail.
- Demo data seeding is behind a property the prod profile sets to `false`, so it structurally cannot run against production.

---

## Testing

**93 tests** — 78 unit, 15 integration. All of them run on every push.

```bash
./mvnw test      # unit tests only (fast, no database needed for most)
./mvnw verify    # unit + integration — what CI runs
```

> Integration tests are named `*IT` and run under **Failsafe**, not Surefire.
> `./mvnw test` alone will silently skip them.

### Unit tests

Service-layer tests with mocked repositories. Deliberately concentrated on the rules that
are invisible in a code read and expensive to get wrong:

| Suite | Covers |
| :--- | :--- |
| `DemandServiceOwnershipTest` | A café cannot read or delete another's demand |
| `CafeServiceOwnershipTest` | A café cannot cancel another's order |
| `SupplierServiceOwnershipTest` | A supplier cannot accept or reject another's match |
| `DailySupplyOwnershipTest` | Catch ownership, and quantity rules against accepted matches |
| `DeliveryTrackingTest` | Status transitions, server-stamped arrival, terminal states |
| `DeliveryRouteTest` | Route grouping, dispatch, completion guards, detach-on-cancel |
| `MatchedOrderStockTest` | A matched order reserves its listing and cannot oversell |
| `StockLedgerTest` | A catch credits its listing, and corrections move both together |
| `AdminServiceDeletionTest` | Self-delete, last-admin and in-flight-order guards |
| `RefreshTokenRotationTest` | Hashing, rotation, replay revocation, suspended accounts |
| `RateLimitServiceTest` | Capacity, key isolation, refill, idle eviction |
| `DemandMatchServiceTest` | Shortfall-only allocation, over-allocation, status recomputation |
| `PriceHistoryTest` | Records real changes only, ignores rescales |
| `NotificationListenerTest` | Mail failure swallowed, no `null` in message bodies |

### Integration tests

Real HTTP over a real migrated PostgreSQL schema. These catch what mocked tests
structurally cannot — and did: an unfiltered market browse was returning 500 because
`LOWER()` on a null bind parameter makes PostgreSQL infer `bytea`. No unit test could have
found it, because they all mock the repository.

- **`OrderLifecycleIT`** — order → accept → deliver → complete → rate, plus double-rating, cancelling a completed order, over-ordering, and paging shape.
- **`ConcurrentOrderIT`** — genuine concurrent transactions fired from a latch, proving the optimistic lock on stock.
- **`RevenueAccountingIT`** — revenue is recognised only on completion. JPQL semantics cannot be proven against a mocked repository, so this runs the real query.
- **`MarketAnalyticsIT`** — the market spread and price trend, for the same reason: aggregates are only meaningful against a real database.
- **`RedisBackedIT`** — the shared limiter and cache against a real Redis. Skipped unless `REDIS_HOST` is set, which CI provides; a skipped test says so rather than a passing one proving nothing.

### On the concurrency tests

A concurrency test can pass simply because the threads serialised, proving nothing. These
were verified by **removing `@Version` and re-running**:

| Scenario | Without the lock |
| :--- | :--- |
| Two 60 kg orders vs 100 kg | Both succeeded — 120 kg sold from 100 |
| Three 8 kg orders vs 10 kg | All three succeeded — 24 kg sold from 10 |
| Eight 5 kg orders | 8 succeeded, only 5 kg reserved — 7 reservations silently lost |

All three tests failed, then passed again on revert. The assertions also hold under either
interleaving — the loser either hits the version conflict or re-reads reduced stock — so
they do not flake.

---

## Continuous Integration

`.github/workflows/ci.yml` runs on every push and pull request to `main`:

- `mvn verify` against a real PostgreSQL service container — the schema is
  PostgreSQL-specific, so an in-memory substitute would not prove much
- JUnit results published to the run summary, including on failure
- A Docker image build, to catch a broken Dockerfile before deploy time

---

## Configuration

Configuration is split so that **no secret is ever committed**:

| File | Committed | Purpose |
| :--- | :--- | :--- |
| `application.properties` | yes | Shared settings. No secrets. |
| `application-dev.properties` | yes | Local defaults so the app runs out of the box. |
| `application-prod.properties` | yes | Every value read from the environment, **no defaults** — a missing variable fails startup rather than silently falling back to a dev value. |
| `application-local.properties` | **no** | Your machine's real credentials. |

The committed profile files contain only `${ENV_VAR}` references, which is why they are safe to track.

> The local override is imported from `application-dev.properties`, not from the base file.
> A profile-specific document outranks the base file *and* everything it imports, so an
> import declared there would lose to the profile's own defaults.

### Production environment variables

| Variable | Notes |
| :--- | :--- |
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | Database connection |
| `JWT_SECRET` | **Minimum 32 bytes.** HS256 rejects anything shorter, and startup fails with an explicit message. |
| `MAIL_USERNAME`, `MAIL_PASSWORD` | SMTP credentials |
| `CORS_ALLOWED_ORIGINS` | Comma-separated allowed origins |
| `RATELIMIT_LOGIN_CAPACITY` | Sign-in attempts per window (default 5) |
| `RATELIMIT_LOGIN_WINDOW` | Window in minutes (default 15) |
| `RATELIMIT_OTP_CAPACITY` | OTP requests per window (default 3) |
| `RATELIMIT_OTP_WINDOW` | Window in minutes (default 15) |
| `MATCH_PENDING_TIMEOUT_HOURS` | How long a supplier has to answer a match (default 24) |
| `ADMIN_EMAIL`, `ADMIN_PASSWORD` | Bootstrap admin — omit to skip creation |
| `NOTIFICATIONS_ENABLED` | Set `false` to suppress outbound email |
| `RATELIMIT_BACKEND` | `memory` (default) or `redis` — shared buckets across instances |
| `CACHE_BACKEND` | `simple` (default) or `redis` — shared caches across instances |
| `REDIS_HOST`, `REDIS_PORT`, `REDIS_ENABLED` | Only needed when either backend is `redis` |

```bash
java -jar target/freshlink-backend-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
```

---

## Database Migrations

Flyway owns the schema in every environment; Hibernate is set to `validate` and only checks that the entities still match.

- Migrations live in `src/main/resources/db/migration`
- Name them `V<n>__short_description.sql`
- **Never edit an applied migration** — Flyway checksums them. Add a new one.

| Migration | What it does |
| :--- | :--- |
| `V1__baseline_schema` | 15 tables, foreign keys, and indexes on every FK column |
| `V2__hash_and_rotate_refresh_tokens` | Token hashing and rotation support |
| `V3__link_orders_to_supply_match` | Traceability from a matched order back to its demand |
| `V4__supply_match_created_at` | Ages matches so abandoned ones can be expired |
| `V5__delivery_tracking` | Driver, ETA, arrival time, and the `IN_TRANSIT`/`FAILED` states |
| `V6__fish_price_history` | Append-only price record |
| `V7__delivery_routes` | Driver routes, and the link from a delivery to its route |

Identifiers are `snake_case` because Spring Boot applies `CamelCaseToUnderscoresNamingStrategy` — `businessRegNo` becomes `business_reg_no`. Do not rename or quote them, or schema validation stops matching.

PostgreSQL does not index foreign keys automatically, so the baseline adds them explicitly.

To reset a local database:

```bash
./mvnw flyway:clean flyway:migrate
```

`clean` is disabled on the `prod` profile.

---

## Notifications

Transactional email on order placed, accepted, rejected, completed, and on every
delivery status change.

Two properties hold regardless of what the mail server does:

- **Sent after commit.** Listeners bind to `AFTER_COMMIT`, so an order that rolls
  back never generates an email announcing it.
- **Failures never propagate.** Notification is a courtesy, not part of the
  transaction. A dead SMTP server logs a warning on the background pool and the
  request is entirely unaffected.

The pool is small and bounded, and drops work rather than running it on the caller
when saturated — a missed courtesy email beats a stalled request thread.

Set `NOTIFICATIONS_ENABLED=false` to turn them off.

---

## Error Responses

Every failure returns a consistent body:

```json
{
  "timestamp": "2026-01-15T10:30:00Z",
  "status": 404,
  "error": "Not Found",
  "message": "Order not found: 42",
  "path": "/api/v1/cafes/orders/42/cancel",
  "fieldErrors": null
}
```

| Status | Meaning |
| :--- | :--- |
| `400` | Validation failure (with `fieldErrors`), malformed body, or a path variable of the wrong type |
| `401` | Missing or invalid access token, or a refresh token that is unknown, expired, already used, or belongs to a suspended account |
| `403` | Authenticated but wrong role, or an account awaiting verification/approval |
| `404` | Not found — **also returned when a resource exists but belongs to someone else**, so IDs cannot be enumerated |
| `409` | Domain rule violation, or a concurrent stock update — safe to retry |
| `429` | Rate limit exceeded on an auth endpoint |
| `500` | Genuinely unexpected. Every domain failure above has a typed exception, so a 500 means a defect, not bad input. Details are logged, never returned. |

---

## Tech Stack

| Layer | Choice |
| :--- | :--- |
| Language | Java 21 |
| Framework | Spring Boot 4.0.1 (Web MVC, Data JPA, Security, Validation, Mail, Actuator) |
| Database | PostgreSQL |
| Migrations | Flyway |
| Auth | Spring Security + JJWT 0.12, hashed rotating refresh tokens |
| Rate limiting | Bucket4j, optionally Redis-backed |
| API docs | springdoc-openapi (Swagger UI) |
| Build | Maven Wrapper, Surefire + Failsafe |
| Testing | JUnit 5, Mockito, AssertJ, MockMvc |
| Container | Docker multi-stage, Docker Compose |
| CI | GitHub Actions |
| Boilerplate | Lombok |

---

## Roadmap

- [ ] WebSocket or push notifications alongside email
- [ ] Demand-side analytics for suppliers deciding what to buy at market

---

## Author

**Sathieskumar Sudeshkar** — BSc (Hons) Software Engineering, Cardiff Metropolitan University

[LinkedIn](https://linkedin.com/in/sathieskumar-sudeshkar) · [Portfolio](https://my-portfolio-v2-five-fawn.vercel.app) · [Email](mailto:sudeshkar008sk@gmail.com)
