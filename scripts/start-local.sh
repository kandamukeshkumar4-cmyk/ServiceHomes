#!/bin/bash
# start-local.sh — One-command startup for ServiceHomes local Docker environment.
# Usage: ./scripts/start-local.sh
# Prerequisites: Docker Desktop, .env file (copy from .env.example)

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
COMPOSE_FILE="$ROOT_DIR/infra/docker/docker-compose.yml"
ENV_FILE="$ROOT_DIR/.env"

if [ ! -f "$ENV_FILE" ]; then
  echo "ERROR: .env file not found at $ENV_FILE"
  echo "Run: cp $ROOT_DIR/.env.example $ENV_FILE"
  exit 1
fi

if ! command -v docker &> /dev/null; then
  echo "ERROR: docker is not installed. Install Docker Desktop first."
  exit 1
fi

echo "Starting ServiceHomes local environment..."
docker compose -f "$COMPOSE_FILE" up --build -d

echo ""
echo "Services starting:"
echo "  PostgreSQL:  localhost:5432"
echo "  LocalStack:  localhost:4566"
echo "  API:         http://localhost:8080"
echo "  Web:         http://localhost:4200"
echo ""
echo "Wait ~30s for API to initialize, then:"
echo "  curl http://localhost:8080/api/health"
echo "  curl http://localhost:4200"
echo ""
echo "To stop: docker compose -f $COMPOSE_FILE down"
