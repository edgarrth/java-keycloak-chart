#!/usr/bin/env bash
set -euo pipefail
set -a
source .env
set +a
python3 infrastructure/seed-keycloak.py
