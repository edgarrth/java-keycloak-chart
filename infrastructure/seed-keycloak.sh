#!/usr/bin/env bash
set -euo pipefail
docker compose --env-file .env -f infrastructure/docker-compose.yml run --rm seed-keycloak
