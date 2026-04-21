# Project Goal
Build a production-style Airbnb-like application without payment processing.

# Canonical Stack
- Frontend: Angular 17, PrimeNG, Leaflet
- Backend: Spring Boot 3, Java 21, PostgreSQL
- Auth: Auth0
- Media: AWS S3
- Analytics: Snowflake + dbt

# Architecture Rules
- Use a modular monolith for the backend.
- Keep domain modules isolated.
- Product OLTP lives in PostgreSQL.
- Analytics never reads directly from UI state.
- All file uploads land in S3, metadata only in PostgreSQL.
- Reservations must prevent overlapping confirmed bookings.
- Every feature must emit analytics events where relevant.

# Engineering Rules
- One vertical slice per branch.
- Every slice includes migration, API contract update, tests, and docs.
- No dead code, placeholder TODOs, or mocked business logic in merged branches.
- Prefer explicit DTOs over leaking entities.
- Keep controllers thin, services transactional, repositories narrow.

# No-Mistakes QA Gate (Mandatory)
- All changes must push through the `no-mistakes` remote, never directly to `origin/master`
- The pipeline will rebase, review, test, update docs, open PR, and babysit CI
- Agent must run `./scripts/push-no-mistakes.sh` after committing changes
- Never force-push to `master`
- Wait for QA pipeline to pass before considering work complete

# Definition of Done
- Build passes
- Tests pass
- Migration included
- API contract updated
- Analytics event updated if applicable
- Docs/ADR updated
- No-mistakes QA gate passed
