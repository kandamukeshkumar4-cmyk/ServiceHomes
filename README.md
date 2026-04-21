# ServiceHomes

A production-style home services and short-term rental platform.

## Repositories

| App | Tech | Path |
|-----|------|------|
| Web | Angular 17 + PrimeNG + Leaflet | `apps/web-angular` |
| API | Spring Boot 3 + Java 21 + PostgreSQL | `apps/api-spring` |
| Analytics | dbt + Snowflake | `apps/analytics-dbt` |

## Quick Start (Local Development — No Auth0 Required)

### Prerequisites

- Java 21
- Node.js 20
- Docker Desktop
- (Optional) dbt + Snowflake for analytics

### 1. Start infrastructure

```bash
make up
```

This starts PostgreSQL (with PostGIS) and LocalStack (S3) via Docker Compose.

### 2. Run backend

```bash
make backend
```

The backend starts on `http://localhost:8080/api` with the `local` profile, which **bypasses Auth0** so you can test immediately.

### 3. Run frontend

```bash
make frontend
```

The Angular app starts on `http://localhost:4200` with a proxy to the backend.

### 4. Open in browser

Go to `http://localhost:4200`

The local dev environment seeds sample data automatically:
- A host user with listings
- A guest user
- Categories and amenities

## Auth0 Setup (Production / Staging)

If you want to use real authentication:

1. Create an Auth0 account at https://auth0.com
2. Create a **Single Page Application** for the Angular frontend
3. Create an **API** for the Spring Boot backend
4. Update these files with your Auth0 credentials:

**`apps/web-angular/src/main.ts`**
```typescript
provideAuth0({
  domain: 'your-tenant.auth0.com',
  clientId: 'YOUR_CLIENT_ID',
  authorizationParams: {
    redirect_uri: window.location.origin,
    audience: 'YOUR_API_IDENTIFIER'
  }
})
```

**`apps/api-spring/src/main/resources/application.yml`**
```yaml
auth0:
  domain: your-tenant.auth0.com
  audience: YOUR_API_IDENTIFIER
```

5. Run without the `local` profile:
```bash
cd apps/api-spring && ./mvnw spring-boot:run
```

## API Endpoints

| Endpoint | Description |
|----------|-------------|
| `GET /api/health` | Health check |
| `GET /api/me` | Current user profile |
| `GET /api/listings/search` | Search published listings |
| `POST /api/listings` | Create listing (host) |
| `GET /api/listings/my` | My listings (host) |
| `POST /api/reservations` | Create reservation |
| `GET /api/reservations/my` | My bookings |

See `packages/api-contracts/openapi.yml` for the full API specification.

## Database Migrations

Migrations run automatically on startup via Flyway:

| Migration | Description |
|-----------|-------------|
| `V0__bootstrap.sql` | Placeholder |
| `V1__auth_identity.sql` | Users, profiles, roles |
| `V2__listings_domain.sql` | Listings, categories, amenities, locations |
| `V3__reservations_base.sql` | Reservations table |
| `V4__reservation_overlap_constraint.sql` | Exclusion constraint for overlap protection |
| `V5__outbox_events.sql` | Analytics outbox events |
| `V6__seed_data.sql` | Sample users and listings |

## Analytics

See [apps/analytics-dbt/README.md](apps/analytics-dbt/README.md).

## Commands

```bash
make help          # Show all commands
make up            # Start Docker services
make down          # Stop Docker services
make backend       # Run Spring Boot (local profile)
make backend-test  # Run backend tests
make frontend      # Run Angular dev server
make frontend-build # Build Angular for production
make dbt-build     # Build dbt models
make dbt-test      # Run dbt tests
make lint          # Run linters
make clean         # Clean build artifacts
```

## Architecture

```
Angular 17 + PrimeNG + Leaflet
    |
    | HTTP /api
    v
Spring Boot 3 (modular monolith)
    ├── identity (users, profiles, roles)
    ├── listings (CRUD, search, categories)
    ├── media (S3 uploads)
    ├── reservations (booking engine)
    └── analytics (outbox events)
    |
    ├── PostgreSQL (OLTP)
    ├── AWS S3 (media + events)
    └── Snowflake + dbt (warehouse)
```

## Docs

- [Architecture Decision Records](docs/adr/)
- [API Contracts](packages/api-contracts/)
- [Event Schemas](packages/event-schemas/)

## License

MIT
