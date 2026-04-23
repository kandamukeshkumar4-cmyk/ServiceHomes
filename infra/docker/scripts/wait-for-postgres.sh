#!/bin/bash
# wait-for-postgres.sh — Block until PostgreSQL is accepting connections.
# Usage: ./wait-for-postgres.sh <host> <port> <timeout_seconds>
# Returns 0 when DB is ready, 1 on timeout.

set -e

HOST="${1:-db}"
PORT="${2:-5432}"
TIMEOUT="${3:-30}"

echo "Waiting for PostgreSQL at ${HOST}:${PORT} (timeout: ${TIMEOUT}s)..."

elapsed=0
while [ $elapsed -lt $TIMEOUT ]; do
  if pg_isready -h "$HOST" -p "$PORT" -q 2>/dev/null; then
    echo "PostgreSQL is ready."
    exit 0
  fi
  sleep 1
  elapsed=$((elapsed + 1))
done

echo "Timed out waiting for PostgreSQL at ${HOST}:${PORT}"
exit 1
