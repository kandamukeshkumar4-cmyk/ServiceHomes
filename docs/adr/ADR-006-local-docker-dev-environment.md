# ADR-006: Docker Compose for Local Development Environment

## Status
Accepted

## Context
The team develops across Windows, macOS, and Linux. Requiring every developer to install Java 21, Node 20, Maven, and PostgreSQL natively creates inconsistency, version drift, and onboarding friction. The existing `make up` / `make backend` / `make frontend` workflow works but depends on host-level toolchain installation.

## Decision
Package both application services (Spring Boot API and Angular frontend) as Docker images and run the full stack via a single `docker compose up` command.

- **API**: Multi-stage Dockerfile (Maven build → JRE runtime), non-root user, health check via `/health` endpoint.
- **Web**: Multi-stage Dockerfile (Node build → Nginx serve), SPA fallback routing, API proxy to backend.
- **Compose**: Single `docker-compose.yml` defines all four services (db, localstack, api, web) on a shared network. Environment variables are sourced from a repo-root `.env` file.
- **Profiles**: The `local` Spring profile remains active in Docker, bypassing Auth0. The datasource URL is overridden via environment variable to point to the `db` service hostname.

## Consequences
- **Positive**: First-time setup is `cp .env.example .env && docker compose up --build`. Identical environments across OS. No local Java/Node required.
- **Positive**: Hot-reload override file provided for developers who want live editing.
- **Negative**: Slower first boot (~2-3 min for image builds).
- **Negative**: Docker Desktop required (already a prerequisite).
- **Negative**: Disk usage increases (~2-3 GB for images and volumes).

## Alternatives Considered
- **Dev containers**: Rejected — adds VS Code dependency and complexity for non-VS Code users.
- **Pre-built images on registry**: Rejected — adds CI complexity and stale image risk for a local-dev-only workflow.
- **Keep native-only workflow**: Rejected — onboarding friction and environment inconsistency are unacceptable for a growing team.

## Date
2026-04-23
