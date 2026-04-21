# ServiceHomes

A production-style home services and short-term rental platform.

## Repositories

| App | Tech | Path |
|-----|------|------|
| Web | Angular 17 + PrimeNG + Leaflet | `apps/web-angular` |
| API | Spring Boot 3 + Java 21 + PostgreSQL | `apps/api-spring` |
| Analytics | dbt + Snowflake | `apps/analytics-dbt` |

## Quick Start

```bash
# Start local infrastructure
cd infra/docker && docker compose up -d

# Run backend
cd apps/api-spring && ./mvnw spring-boot:run

# Run frontend
cd apps/web-angular && npm install && ng serve
```

## Docs

- [Architecture Decision Records](docs/adr/)
- [API Contracts](packages/api-contracts/)
- [Event Schemas](packages/event-schemas/)

## Analytics

See [apps/analytics-dbt/README.md](apps/analytics-dbt/README.md).

## License

MIT
