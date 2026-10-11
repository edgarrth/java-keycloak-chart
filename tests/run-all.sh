#!/usr/bin/env bash
set -euo pipefail
mkdir -p .runtime
compose=(docker compose --env-file .env -f infrastructure/docker-compose-tests.yml)
bash infrastructure/build-base.sh
"${compose[@]}" run --rm --no-deps unit-tests
"${compose[@]}" run --rm --no-deps test-runner
bash tests/run-ha.sh
