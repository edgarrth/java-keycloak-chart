#!/usr/bin/env bash
set -euo pipefail
docker compose --env-file .env -f infrastructure/docker-compose.yml up postgres-keycloak postgres-external
