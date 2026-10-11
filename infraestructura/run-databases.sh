#!/usr/bin/env bash
set -euo pipefail
docker compose --env-file .env -f infraestructura/docker-compose.yml up postgres-keycloak postgres-external
