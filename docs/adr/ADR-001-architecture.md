# ADR-001: Modular Monolith Architecture

## Status
Accepted

## Context
We need to build an Airbnb-like platform with clear domain boundaries but without the operational complexity of microservices on day one.

## Decision
Use a modular monolith architecture:
- One Spring Boot deployable
- Internal modules for identity, listings, media, search, reservations, analytics outbox
- PostgreSQL as single OLTP store
- S3 for media
- Angular frontend as separate deployable
- dbt + Snowflake as separate analytics stack

## Consequences
- Easier to develop, test, and deploy initially
- Clear module boundaries allow future extraction
- No distributed transaction complexity
- Must enforce module boundaries through code review

## Alternatives Considered
- Microservices: rejected due to overhead for solo/small team
- Serverless: rejected due to cold start and vendor lock-in concerns

## Date
2026-04-21
