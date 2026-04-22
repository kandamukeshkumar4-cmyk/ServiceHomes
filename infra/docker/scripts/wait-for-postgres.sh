#!/bin/sh

set -eu

POSTGRES_HOST="${POSTGRES_HOST:-db}"
POSTGRES_PORT="${POSTGRES_PORT:-5432}"
POSTGRES_USER="${POSTGRES_USER:-servicehomes}"
POSTGRES_DB="${POSTGRES_DB:-servicehomes}"

until pg_isready -h "$POSTGRES_HOST" -p "$POSTGRES_PORT" -U "$POSTGRES_USER" -d "$POSTGRES_DB"; do
  echo "Waiting for PostgreSQL at ${POSTGRES_HOST}:${POSTGRES_PORT}..."
  sleep 2
done

echo "PostgreSQL is ready."
