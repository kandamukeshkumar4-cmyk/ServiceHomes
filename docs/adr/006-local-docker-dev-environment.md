# ADR 006: Local Docker Dev Environment

- Status: Accepted
- Date: 2026-04-22

## Context

The ServiceHomes stack spans Angular 17, Spring Boot 3, PostgreSQL, and S3-compatible object storage. The team works across Windows, macOS, and Linux, and local setup currently depends on developers installing and maintaining Java 21, Node 20, PostgreSQL, and related tooling on their host machines.

That increases onboarding time and makes local integration bugs harder to reproduce consistently.

## Decision

Adopt Docker Compose as the canonical local packaging workflow for the product stack:

- PostgreSQL and MinIO continue to run as containers.
- The Spring Boot API ships with a multi-stage Docker build and starts in the `local` profile.
- The Angular web app ships with a multi-stage Docker build and serves the production bundle through Nginx with SPA routing and `/api/` proxying.
- Root-level `.env.example` files define overridable local defaults without committing machine-specific secrets.

## Consequences

- First boot is slower because images must build before the stack comes up.
- Local environments become consistent across operating systems and no longer require host-installed Java or Node.
- Native workflows remain available for developers who want faster inner-loop iteration.
- Docker Compose becomes the preferred path for local integration testing and validating cross-service behavior.
