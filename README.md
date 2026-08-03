# 🐟 FreshLink — B2B Fish Supply Backend

> A Spring Boot REST API connecting fish suppliers with cafés and restaurants in Kandy, Sri Lanka — replacing informal phone-and-WhatsApp ordering with a structured marketplace and a tracked order lifecycle.

<p align="left">
  <img src="https://img.shields.io/badge/Java-21-ED8B00?style=flat-square&logo=openjdk&logoColor=white" alt="Java 21"/>
  <img src="https://img.shields.io/badge/Spring%20Boot-4.0.1-6DB33F?style=flat-square&logo=springboot&logoColor=white" alt="Spring Boot"/>
  <img src="https://img.shields.io/badge/PostgreSQL-316192?style=flat-square&logo=postgresql&logoColor=white" alt="PostgreSQL"/>
  <img src="https://img.shields.io/badge/Flyway-CC0200?style=flat-square&logo=flyway&logoColor=white" alt="Flyway"/>
  <img src="https://img.shields.io/badge/Spring%20Security-JWT-6DB33F?style=flat-square&logo=springsecurity&logoColor=white" alt="Spring Security"/>
  <img src="https://img.shields.io/badge/Maven-C71A36?style=flat-square&logo=apachemaven&logoColor=white" alt="Maven"/>
</p>

---

## The Problem

Small café owners in Kandy source fish through informal daily phone calls. There's no price visibility, no record of what was ordered, and no accountability when a delivery falls short. Suppliers, in turn, have no reliable read on demand before they buy at the market.

FreshLink puts that exchange behind an API: suppliers publish what they have, cafés browse and order, and every order carries a status that both sides can see.

It runs two complementary trade flows:

- **Spot market** — suppliers list available catch, cafés browse and order it directly.
- **Demand matching** — cafés post forward demand for a future date, and a scheduled engine pairs it against suppliers' daily catch, ranked by freshness and supplier rating.

---

## Features

- **Three-role access model** — `SUPPLIER`, `CAFE`, and `ADMIN`, enforced with method-level `@PreAuthorize` guards on every controller.
- **Ownership enforced in the service layer** — a café can only ever read or modify its own orders and demand; a supplier only its own listings and matches. Checks live below the controller so they cannot be bypassed by a new caller.
- **JWT authentication with refresh tokens** — short-lived access tokens backed by a persisted refresh-token store, so sessions survive without long-lived credentials.
- **Email OTP verification** — 6-digit codes via SMTP, 5-minute expiry, capped retry attempts before invalidation.
- **Supplier fish catalogue** — full CRUD over listings, scoped so a supplier can only touch their own inventory.
- **Café marketplace search** — browse available fish filtered by species and city. Suspended and removed suppliers never appear.
- **Tracked order lifecycle** — orders move through accept, reject, delivering, and completion transitions, each a distinct authorised endpoint.
- **Concurrency-safe stock** — reservations are guarded by an optimistic-locking version column, so two simultaneous orders cannot claim the same fish.
- **Demand matching engine** — greedy allocation ranked by freshness, then supplier rating, then catch time; re-runs every 10 minutes for unfilled demand.
- **Post-delivery ratings** — cafés rate suppliers once an order completes, feeding both the leaderboard and the matching engine's ranking.
- **Admin oversight** — account activation, soft deletion with safeguards, and an analytics dashboard.
- **Versioned schema** — Flyway owns every table; Hibernate only validates.

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
├── security/       # SecurityConfig, JwtFilter, UserPrincipal
├── exception/      # Typed exceptions + global handler
├── util/           # JwtUtil
├── enums/          # Role, OrderStatus, DemandStatus, MatchStatus, ...
├── mapper/         # Entity ↔ DTO mapping
├── config/, cache/ # Caching and scheduled eviction
└── *dto/           # Request/response DTOs, grouped by domain
```

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
| `POST` | `/auth/refresh` | Exchange a refresh token for a new access token |
| `POST` | `/auth/logout` | Invalidate the refresh token |

New accounts are created inactive and require **both** email verification and admin activation before login succeeds.

### Supplier — `/suppliers` · role `SUPPLIER`

| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `GET` | `/suppliers/me` | Current supplier profile |
| `POST` | `/suppliers/fish` | Add a fish listing |
| `GET` | `/suppliers/fish` | List own listings |
| `PUT` | `/suppliers/fish/{id}` | Update a listing |
| `DELETE` | `/suppliers/fish/{id}` | Remove a listing |
| `GET` | `/suppliers/orders` | Incoming orders |
| `PUT` | `/suppliers/orders/{orderId}/accept` | Accept an order |
| `PUT` | `/suppliers/orders/{orderId}/reject` | Reject an order |
| `PUT` | `/suppliers/orders/{orderId}/markdelivering` | Mark as out for delivery |
| `PUT` | `/suppliers/orders/{orderId}/complete` | Mark as delivered |
| `GET` | `/suppliers/supply-matches` | Pending demand matches |
| `PUT` | `/suppliers/supply-matches/{id}/accept` | Accept a match — deducts supply, creates an order |
| `PUT` | `/suppliers/supply-matches/{id}/reject` | Reject a match — returns demand to the pool |

### Café — `/cafes` · role `CAFE`

| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `GET` | `/cafes/me` | Current café profile |
| `GET` | `/cafes/market/fish` | Browse the marketplace — optional `fishType`, `city` filters |
| `POST` | `/cafes/orders` | Place an order (single supplier per order) |
| `GET` | `/cafes/orders` | Order history |
| `PUT` | `/cafes/orders/{orderId}/cancel` | Cancel while still pending |
| `POST` | `/cafes/orders/{orderId}/rate` | Rate the supplier after completion, 1–5 |

### Demand — `/demand` · role `CAFE`

| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `POST` | `/demand/create` | Post a forward demand request |
| `GET` | `/demand` | List own demand |
| `DELETE` | `/demand/{demandId}/delete` | Withdraw a demand request |

### Admin — `/admin` · role `ADMIN`

| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `GET` | `/admin/suppliers` | List all suppliers |
| `GET` | `/admin/cafes` | List all cafés |
| `PUT` | `/admin/users/{id}/activate` | Activate a user account |
| `DELETE` | `/admin/users/{id}/delete` | Soft-delete an account |
| `GET` | `/admin/analytics/dashboard` | Revenue, order and demand metrics |
| `GET` | `/admin/analytics/suppliers` | Supplier performance leaderboard |

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

Matching runs on creation and again every 10 minutes for demand still open or partially filled.

---

## Getting Started

### Prerequisites

- JDK 21+
- PostgreSQL 14+
- An SMTP account for OTP delivery (Gmail app passwords work)

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

# Optional — needed only if you want OTP emails to actually send
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

Run the tests:

```bash
./mvnw test
```

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

## Configuration

Configuration is split so that **no secret is ever committed**:

| File | Committed | Purpose |
| :--- | :--- | :--- |
| `application.properties` | yes | Shared settings. No secrets. |
| `application-dev.properties` | yes | Local defaults so the app runs out of the box. |
| `application-prod.properties` | yes | Every value read from the environment, **no defaults** — a missing variable fails startup rather than silently falling back to a dev value. |
| `application-local.properties` | **no** | Your machine's real credentials. |

The committed profile files contain only `${ENV_VAR}` references, which is why they are safe to track.

### Production environment variables

| Variable | Notes |
| :--- | :--- |
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | Database connection |
| `JWT_SECRET` | **Minimum 32 bytes.** HS256 rejects anything shorter, and startup fails with an explicit message. |
| `MAIL_USERNAME`, `MAIL_PASSWORD` | SMTP credentials for OTP delivery |
| `CORS_ALLOWED_ORIGINS` | Comma-separated allowed origins |
| `ADMIN_EMAIL`, `ADMIN_PASSWORD` | Bootstrap admin — omit to skip creation |

```bash
java -jar target/freshlink-backend-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
```

---

## Database Migrations

Flyway owns the schema in every environment; Hibernate is set to `validate` and only checks that the entities still match.

- Migrations live in `src/main/resources/db/migration`
- Name them `V<n>__short_description.sql`
- **Never edit an applied migration** — Flyway checksums them. Add a new one.

`V1__baseline_schema.sql` is the full baseline: 15 tables, foreign keys, and indexes on every FK column (PostgreSQL does not create those automatically).

Identifiers are `snake_case` because Spring Boot applies `CamelCaseToUnderscoresNamingStrategy` — `businessRegNo` becomes `business_reg_no`. Do not rename or quote them, or schema validation stops matching.

To reset a local database:

```bash
./mvnw flyway:clean flyway:migrate
```

`clean` is disabled on the `prod` profile.

---

## Account Lifecycle

Accounts are **soft-deleted** — the row is retained so orders, ratings, and analytics keep their references and trade history stays intact. Deletion is refused when the account is the last remaining admin, is the acting admin's own account, or still has orders in progress.

Removed suppliers are excluded from the market, from matching, and from direct ordering by fish id. Email addresses are deliberately not released, so a removed supplier cannot re-register on the same address to shed its rating history.

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
| `400` | Validation failure (with `fieldErrors`) |
| `401` | Missing or invalid token |
| `403` | Authenticated but wrong role |
| `404` | Not found — **also returned when a resource exists but belongs to someone else**, so IDs cannot be enumerated |
| `409` | Domain rule violation, or a concurrent stock update — safe to retry |
| `500` | Unexpected. Details are logged, never returned. |

---

## Tech Stack

| Layer | Choice |
| :--- | :--- |
| Language | Java 21 |
| Framework | Spring Boot 4.0.1 (Web MVC, Data JPA, Security, Validation, Mail) |
| Database | PostgreSQL |
| Migrations | Flyway |
| Auth | Spring Security + JJWT, persisted refresh tokens |
| Build | Maven Wrapper |
| Testing | JUnit 5, Mockito, AssertJ |
| Boilerplate | Lombok |

---

## Roadmap

- [ ] Supplier endpoint for recording daily supply (currently seeded only)
- [ ] Rate limiting on login and OTP request endpoints
- [ ] Hashed, rotating refresh tokens
- [ ] Delivery assignment and route grouping
- [ ] Integration test coverage across the order lifecycle
- [ ] Pagination on list endpoints
- [ ] OpenAPI/Swagger documentation
- [ ] Containerised deployment

---

## Author

**Sathieskumar Sudeshkar** — BSc (Hons) Software Engineering, Cardiff Metropolitan University

[LinkedIn](https://linkedin.com/in/sathieskumar-sudeshkar) · [Portfolio](https://my-portfolio-v2-five-fawn.vercel.app) · [Email](mailto:sudeshkar008sk@gmail.com)
