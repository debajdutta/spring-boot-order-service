# order-service

A Spring Boot REST API for managing orders, built as a learning and portfolio project.
It is developed in phases: **phase 1 (this version)** delivers the API with no authentication;
**phase 2** adds OAuth2 security with Microsoft Entra ID and role-based access.

**Stack:** Java 21 · Spring Boot 4.2 (Spring Framework 7) · Spring Data JPA / Hibernate · MySQL 8 · H2 (tests) · Lombok · Maven

## Features

- CRUD API for orders with a **status lifecycle** enforced in the domain (`CREATED → CONFIRMED → SHIPPED → DELIVERED`, or `→ CANCELLED`)
- **Search endpoint** with its own API vocabulary, explicit request/response mapping and a hand-written JPQL query
- **API versioning** in the URL (`/api/v1/...`) using Spring Framework 7's built-in support
- **Consistent errors** as RFC 9457 Problem Details (400 validation, 404, 409 business rules and concurrent edits)
- **Optimistic locking** (`@Version`) so concurrent updates can't silently overwrite each other
- **Actuator** endpoints for health (including DB), info, metrics and mappings
- **100 sample orders** generated on startup for realistic testing
- **21 integration tests** against an in-memory database, plus a Postman collection

## Quick start

Prerequisites: JDK 21, and MySQL 8 reachable on `localhost:3306` (Docker is fine).

```bash
# 1. Create the database (once). Run in your MySQL container/client as root:
#    CREATE DATABASE IF NOT EXISTS orders_db;
#    Optional but recommended - a dedicated app user instead of root:
#    CREATE USER 'orders_app'@'%' IDENTIFIED BY '<password>';
#    GRANT ALL PRIVILEGES ON orders_db.* TO 'orders_app'@'%';

# 2. Configure credentials (git-ignored file)
cp .env.example .env.local      # then edit DB_USERNAME / DB_PASSWORD

# 3. Run tests, then start the app on http://localhost:8080
./start.sh                      # or: ./start.sh --skip-tests
```

Tables are created automatically, and the table is topped up to 100 sample orders on first start.

Check it is up: <http://localhost:8080/actuator/health> should report `"status":"UP"` with the database `UP`.

**Postman:** import both files in [`postman/`](postman/), select the **order-service local** environment and run the collection
(happy path, error cases, search and versioning folders).

## API

Base path: `/api/v1`. Request and response bodies are JSON.

| Method | Path | Purpose | Success | Errors |
|---|---|---|---|---|
| GET | `/orders?status=&page=&size=&sort=` | List orders (paged, optional status filter) | 200 | 400 |
| GET | `/orders/{id}` | Get one order | 200 | 404 |
| POST | `/orders` | Create an order | 201 + `Location` | 400 |
| PUT | `/orders/{id}` | Replace editable fields (only while `CREATED`) | 200 | 400, 404, 409 |
| PATCH | `/orders/{id}/status` | Change status (`{"status":"CONFIRMED"}`) | 200 | 400, 404, 409 |
| DELETE | `/orders/{id}` | Delete an order | 204 | 404 |
| GET | `/orders/search?...` | Search with filters, sorting and paging (below) | 200 | 400 |

Create request example:

```json
{ "customerName": "Jane Doe", "product": "27-inch Monitor", "quantity": 2, "unitPrice": 249.99 }
```

Ops endpoints (not versioned): `/actuator/health`, `/actuator/info`, `/actuator/metrics`, `/actuator/mappings`.

### Search

`GET /api/v1/orders/search` deliberately uses an API vocabulary that differs from the database model,
and translates it explicitly:

| Parameter | Meaning | Notes |
|---|---|---|
| `customer` | customer name contains | case-insensitive; `%` and `_` are matched literally |
| `item` | product name contains | case-insensitive |
| `state` | `open` (CREATED + CONFIRMED), `in_transit`, `delivered`, `cancelled` | coarser than the internal status |
| `minTotal`, `maxTotal` | order total range | total = quantity × unit price (computed) |
| `placedFrom`, `placedTo` | date range, e.g. `2026-11-01` | UTC calendar days, both inclusive |
| `orderBy` | `placed` (default), `total`, `customer`, `item` | whitelist; anything else is a 400 |
| `direction` | `asc` or `desc` (default) | |
| `limit`, `offset` | page size 1-100 (default 20), offset ≥ 0 | |

Example: `/api/v1/orders/search?state=open&minTotal=200&orderBy=total&direction=desc&limit=10`

The response contains `results[]` (orderNumber, customer, item, quantity, total, state, placedOn, href),
`paging` (offset, limit, totalMatches, hasMore) and the applied `sort`. All invalid parameters are reported together in one 400.

### Errors

Every error uses the same Problem Details shape:

```json
{
  "type": "about:blank",
  "title": "Validation error",
  "status": 400,
  "detail": "Request validation failed",
  "instance": "/api/v1/orders",
  "errors": { "quantity": "must be greater than or equal to 1" }
}
```

### Versioning

The version is part of the path (`/api/v1/...`) and each handler declares the version it serves,
e.g. `@GetMapping(path = "/{id}", version = "1")`, so a v2 handler can be added next to v1 later.
Unsupported versions such as `/api/v2/...` return 400. See `config/ApiVersioningConfig`.

## Design

```
io.github.debajdutta.orderservice
├── order
│   ├── domain/       Order entity, OrderStatus (lifecycle rules)
│   ├── dto/          Request/response records with Bean Validation
│   ├── repository/   Spring Data JPA repository
│   ├── service/      Business logic and transactions
│   ├── web/          REST controller
│   └── search/       Search feature as a vertical slice (see below)
├── common
│   ├── exception/    Exceptions + GlobalExceptionHandler (Problem Details)
│   └── web/          PageResponse, CurrentActorProvider
└── config/           API versioning, sample data, phase-1 actor
```

Key decisions:

- **DTOs, not entities, over HTTP.** Records with Bean Validation for requests and responses; the JPA entity never leaves the service layer.
- **CRUD is layered; search is a vertical slice.** `order/search` holds everything the search feature needs: controller, request and response types, mappers, API vocabulary and its own `EntityManager` query. Only the controller and request/response types are public; the rest is package-private, so the compiler enforces the boundary. It shares only the `Order` entity with the rest of the app.
- **Explicit mapping in search.** `request (API terms) → criteria (domain terms) → JPQL → entities → response (API terms)`. Every filter value is a bound parameter, and sorting only uses whitelisted expressions, so user input never reaches the SQL text.
- **Rules live in the domain.** Allowed status transitions are defined on `OrderStatus`; only `CREATED` orders can be edited.
- **Ready for security.** `createdBy`/`updatedBy` come from `CurrentActorProvider`. Phase 1 returns `anonymous`; phase 2 swaps in an implementation that reads the user from the JWT, without changing business code.

## Configuration

Settings are in `src/main/resources/application.properties`; database values come from environment variables
(loaded from `.env.local` by `start.sh`).

| Variable / property | Default | Purpose |
|---|---|---|
| `DB_HOST`, `DB_PORT`, `DB_NAME` | `localhost`, `3306`, `orders_db` | MySQL location |
| `DB_USERNAME` | `orders_app` | MySQL user |
| `DB_PASSWORD` | *(none, required)* | MySQL password |
| `app.seed-data` | `true` | Generate sample orders on startup |
| `app.seed-order-count` | `100` | Number of orders to top the table up to |

Sample data is deterministic and existing rows are never changed. To regenerate: `TRUNCATE TABLE orders;` and restart.

## Testing

```bash
./mvnw test
```

Tests use an in-memory H2 database in MySQL mode, so no database server is needed.
They cover CRUD, validation, status rules, paging, versioning, actuator health and every search filter and error case.

## Roadmap

- [x] **Phase 1:** REST API, search, versioning, error handling, tests, Postman collection
- [ ] **Phase 2:** OAuth2 Resource Server with Microsoft Entra ID. App roles `Orders.Reader` (read and search), `Orders.Writer` (create, update, change status) and `Orders.Admin` (delete, ops endpoints); 401/403 as Problem Details; real user ids in `createdBy`/`updatedBy`
- [ ] Database migrations with Flyway (replace `ddl-auto=update`)
- [ ] OpenAPI documentation
- [ ] Container image and CI pipeline
