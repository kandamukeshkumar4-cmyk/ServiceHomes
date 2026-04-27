# ServiceHomes

## Guest Wishlists, Saved Searches, and Recently Viewed

The wishlist platform lets guests create collaborative listing collections, save search filter sets, and return to recently viewed listings. Public share links are read-only, collaborator access can add and edit wishlist items, and recent views are retained for 90 days before scheduled cleanup.

A production-style home services and short-term rental platform.

## Repositories

| App | Tech | Path |
|-----|------|------|
| Web | Angular 17 + PrimeNG + Leaflet | `apps/web-angular` |
| API | Spring Boot 3 + Java 21 + PostgreSQL | `apps/api-spring` |
| Analytics | dbt + Snowflake | `apps/analytics-dbt` |

## Quick Start (Docker)

### Prerequisites

- Docker Desktop

### 1. Create local env files

```bash
cp .env.example .env
cp .env.local.example .env.local
```

The `local` Spring profile keeps Auth0 disabled, so the placeholder Auth0 values in `.env` do not block local startup.

### 2. Start the full stack

```bash
docker compose -f infra/docker/docker-compose.yml up --build
```

Optional wrapper:

```bash
./scripts/start-local.sh
```

### 3. Verify the stack

```bash
curl http://localhost:8080/health
curl http://localhost:4200
```

- `http://localhost:8080/health` should return a `200` health payload.
- `http://localhost:4200` should return the Angular `index.html`.
- Nginx proxies `/api/*` traffic from the web container to the Spring Boot API container.

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

This starts PostgreSQL (with PostGIS) and MinIO (S3-compatible storage) via Docker Compose for the native workflow.

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

## No-Mistakes QA Gate (Required for All PRs)

This repo uses [no-mistakes](https://github.com/kunchenguid/no-mistakes) as a mandatory QA pipeline. **Every commit must go through the gate.**

### Quick setup

```bash
# One-time installation
./scripts/install-no-mistakes.sh

# Daily workflow
git checkout -b feat/my-feature
# ... make changes ...
git commit -am "feat: my feature"
./scripts/push-no-mistakes.sh   # pushes through QA gate
```

### What the gate does

1. **Rebase** onto latest `master`
2. **Agentic review** — AI finds bugs, style issues, security problems
3. **Test** — Spring Boot + Angular + dbt full test suite
4. **Docs** — README, ADRs, API contracts auto-updated
5. **PR** — clean PR opened automatically
6. **CI** — GitHub Actions monitored, failures auto-fixed

See [NO-MISTAKES.md](NO-MISTAKES.md) for full documentation.

## Auth0 Setup (Production / Staging)

If you want to use real authentication:

1. Create an Auth0 account at https://auth0.com
2. Create a **Single Page Application** for the Angular frontend
3. Create an **API** for the Spring Boot backend
4. Update these files with your Auth0 credentials:

**`apps/web-angular/src/environments/environment.ts`**
```typescript
export const environment = {
  production: true,
  apiBaseUrl: '/api',
  auth: {
    enabled: true,
    domain: 'your-tenant.auth0.com',
    clientId: 'YOUR_CLIENT_ID',
    audience: 'YOUR_API_IDENTIFIER',
    useRefreshTokens: true,
    cacheLocation: 'localstorage' as const
  }
};
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

`apps/web-angular/src/environments/environment.development.ts` keeps Auth0 disabled for local Angular development so the frontend can use the backend's local auth bypass out of the box.

## API Endpoints

| Endpoint | Description |
|----------|-------------|
| `GET /health` | Health check |
| `GET /api/me` | Current user profile |
| `PATCH /api/me/profile` | Update current user's profile |
| `POST /api/me/profile/avatar-upload-url` | Get a presigned avatar upload target |
| `POST /api/me/become-host` | Enable hosting for the current user |
| `GET /api/hosts/{hostId}` | Public host profile and stats |
| `GET /api/listings/search` | Search published listings |
| `POST /api/listings` | Create listing (host) |
| `GET /api/listings/my` | My listings (host) |
| `GET /api/listings/{id}/availability` | Get host availability rules |
| `PUT /api/listings/{id}/availability` | Replace host availability rules |
| `GET /api/listings/{id}/calendar` | Get host calendar day view |
| `POST /api/reservations` | Create reservation |
| `GET /api/reservations/my` | My bookings |
| `POST /api/reservations/{id}/accept` | Accept a pending booking request (host) |
| `POST /api/reservations/{id}/decline` | Decline a pending booking request (host) |
| `POST /api/reservations/{id}/review` | Create a guest review for a completed stay |
| `POST /api/reservations/{id}/host-review` | Create a host review for a completed stay |
| `GET /api/listings/{id}/reviews` | List visible listing reviews and rating aggregates |
| `POST /api/reviews/{id}/response` | Add one host response to a visible guest review |
| `POST /api/reviews/{id}/report` | Report a visible review for moderation |

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
| `V7__align_local_seed_auth0_id.sql` | Align local JWT identity with the seeded local user |
| `V8__listing_availability_indexes.sql` | Availability rule query indexes |
| `V9__profile_fields.sql` | Host/profile enrichment fields and profile languages |
| `V10__search_indexes.sql` | Listing search indexes |
| `V11__reviews.sql` | Base reviews table |
| `V12__saved_listings.sql` | Saved listings |
| `V13__messaging.sql` | Guest-host messaging |
| `V14__reviews_ratings_platform.sql` | Double-blind reviews, rating cache, reports, moderation |

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

## User Profiles and Hosting

- The account page now supports editing display name, bio, phone number, location, languages, and avatar uploads through a presigned S3 flow.
- `POST /api/me/become-host` promotes an email-verified traveler account by adding the `HOST` role without removing traveler access.
- Public host profiles at `/hosts/{hostId}` expose biography, membership age, response-rate stats, and the host's published listings.

## License

MIT
