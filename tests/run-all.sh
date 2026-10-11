#!/usr/bin/env bash
set -euo pipefail
set -a
source .env
set +a
mvn -B -ntp verify spotless:check
python3 tests/e2e.py
