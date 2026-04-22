#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ENV_FILE="${ROOT_DIR}/.env"

if [[ ! -f "${ENV_FILE}" ]]; then
  echo "Missing ${ENV_FILE}. Copy .env.example to .env before starting the stack."
  exit 1
fi

docker compose --env-file "${ENV_FILE}" -f "${ROOT_DIR}/infra/docker/docker-compose.yml" up --build "$@"
