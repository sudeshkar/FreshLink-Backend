# 🐟 FreshLink — B2B Fish Supply Backend

> A Spring Boot REST API connecting fish suppliers with cafés and restaurants in Kandy, Sri Lanka — replacing informal phone-and-WhatsApp ordering with a structured marketplace and a tracked order lifecycle.

<p align="left">
  <img src="https://img.shields.io/badge/Java-21-ED8B00?style=flat-square&logo=openjdk&logoColor=white" alt="Java 21"/>
  <img src="https://img.shields.io/badge/Spring%20Boot-4.0.1-6DB33F?style=flat-square&logo=springboot&logoColor=white" alt="Spring Boot"/>
  <img src="https://img.shields.io/badge/PostgreSQL-316192?style=flat-square&logo=postgresql&logoColor=white" alt="PostgreSQL"/>
  <img src="https://img.shields.io/badge/Spring%20Security-JWT-6DB33F?style=flat-square&logo=springsecurity&logoColor=white" alt="Spring Security"/>
  <img src="https://img.shields.io/badge/Maven-C71A36?style=flat-square&logo=apachemaven&logoColor=white" alt="Maven"/>
</p>

---

## The Problem

Small café owners in Kandy source fish through informal daily phone calls. There's no price visibility, no record of what was ordered, and no accountability when a delivery falls short. Suppliers, in turn, have no reliable read on demand before they buy at the market.

FreshLink puts that exchange behind an API: suppliers publish what they have, cafés browse and order, and every order carries a status that both sides can see.

---

## Features

- **Three-role access model** — `SUPPLIER`, `CAFE`, and `ADMIN`, enforced with method-level `@PreAuthorize` guards on every controller.
- **JWT authentication with refresh tokens** — short-lived access tokens backed by a persisted refresh-token store, so sessions survive without long-lived credentials.
- **Email OTP verification** — 6-digit codes via SMTP, 5-minute expiry, capped retry attempts before invalidation.
- **Supplier fish catalogue** — full CRUD over listings, scoped so a supplier can only touch their own inventory.
- **Café marketplace search** — browse available fish filtered by species and city.
- **Tracked order lifecycle** — orders move through accept, reject, delivering, and completion transitions, each a distinct authorised endpoint.
- **Post-delivery ratings** — cafés rate suppliers once an order completes, building a reputation signal.
- **Admin oversight** — account listings and user activation.
- **Seeded reference data** — fish types with seasonal availability windows are bootstrapped on first run.

---

## Architecture

A conventional layered Spring Boot structure, with interfaces separating contracts from implementations:

```
Controller  →  Service (interface)  →  ServiceImpl  →  Repository  →  PostgreSQL
     ↓                                       ↓
    DTO   ←────────────  Mapper  ←────────  Entity
```

Entities never cross the controller boundary. Dedicated request/response DTOs and hand-written mappers (`CafeMapper`, `FishMapper`, `OrderMapper`, `SupplierMapper`, `UserDtoMapper`) keep the persistence model out of the API surface — which also avoids the bidirectional-relationship JSON recursion that plagues naive JPA REST layers.

```
src/main/java/com/freshlink/
├── controller/     # Auth, Cafe, Supplier, Admin
├── service/
│   └── interfaces/ # Service contracts
│       └── impl/   # Implementations
├── Repository/     # Spring Data JPA repositories
├── model/          # JPA entities
├── security/       # SecurityConfig, JwtFilter, UserPrincipal
├── util/           # JwtUtil
├── enums/          # Role, OrderStatus, DeliveryStatus, ...
├── mapper/         # Entity ↔ DTO mapping
└── *dto/           # Request/response DTOs, grouped by domain
```

---

## API Reference

Base path: `/api/v1`

### Authentication — `/auth`

| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `POST` | `/auth/register/cafe` | Register a café account |
| `POST` | `/auth/register/supplier` | Register a supplier account |
| `POST` | `/auth/request-otp` | Send a verification OTP to an email |
| `POST` | `/auth/verfy-otp` | Confirm the OTP and verify the account |
| `POST` | `/auth/login` | Authenticate, receive access + refresh tokens |
| `POST` | `/auth/refresh` | Exchange a refresh token for a new access token |
| `POST` | `/auth/logout` | Clear the security context |

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
| `PUT` | `/suppliers/orders/{orderId}/completeorder` | Mark as delivered |

### Café — `/cafes` · role `CAFE`

| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `GET` | `/cafes/me` | Current café profile |
| `GET` | `/cafes/market/fish` | Browse the marketplace — optional `fishType`, `city` filters |
| `POST` | `/cafes/orders` | Place an order |
| `GET` | `/cafes/orders` | Order history |
| `PUT` | `/cafes/orders/{orderId}/cancel` | Cancel an order |
| `POST` | `/cafes/orders/{orderId}/rate` | Rate the supplier after completion |

### Admin — `/admin` · role `ADMIN`

| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `GET` | `/admin/suppliers` | List all suppliers |
| `GET` | `/admin/cafes` | List all cafés |
| `PUT` | `/admin/users/{id}/activate` | Activate a user account |

---

## Order Lifecycle

```
   Café places order
          │
          ▼
      [ PENDING ] ──── café cancels ────▶ [ CANCELLED ]
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
   completeorder
       ▼
  [ COMPLETED ] ────▶ café submits rating
```

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

Create the database:

```sql
CREATE DATABASE freshlink;
```

Add `src/main/resources/application.properties` — this file is gitignored, so create it locally:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/freshlink
spring.datasource.username=YOUR_DB_USER
spring.datasource.password=YOUR_DB_PASSWORD

spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true

# JWT — use a long, random, base64-safe secret
jwt.secret=YOUR_JWT_SECRET
jwt.expiration=3600000

# SMTP for OTP email
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=YOUR_EMAIL
spring.mail.password=YOUR_APP_PASSWORD
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
```

Run it:

```bash
./mvnw spring-boot:run        # macOS / Linux
mvnw.cmd spring-boot:run      # Windows
```

The API comes up on `http://localhost:8080`.

On first start the application seeds a default admin account and a set of fish types. Change the admin password immediately in any non-local environment.

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

## Tech Stack

| Layer | Choice |
| :--- | :--- |
| Language | Java 21 |
| Framework | Spring Boot 4.0.1 (Web MVC, Data JPA, Security, Validation, Mail) |
| Database | PostgreSQL |
| Auth | Spring Security + JJWT, refresh-token rotation |
| Build | Maven Wrapper |
| Boilerplate | Lombok |

---

## Roadmap

The demand-matching layer is modelled but not yet wired up — `DemandRequest`, `DailySupply`, `SupplyMatch` and `Delivery` entities exist alongside their status enums, with the service and endpoint layer still to come:

- [ ] Demand aggregation — cafés post next-day requirements
- [ ] Supply matching engine to pair daily supply against demand and cut manual coordination
- [ ] Delivery assignment and route grouping
- [ ] Integration test coverage across the order lifecycle
- [ ] OpenAPI/Swagger documentation
- [ ] Containerised deployment

---

## Author

**Sathieskumar Sudeshkar** — BSc (Hons) Software Engineering, Cardiff Metropolitan University

[LinkedIn](https://linkedin.com/in/sathieskumar-sudeshkar) · [Portfolio](https://my-portfolio-v2-five-fawn.vercel.app) · [Email](mailto:sudeshkar008sk@gmail.com)
