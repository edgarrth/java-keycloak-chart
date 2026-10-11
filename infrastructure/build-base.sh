#!/usr/bin/env bash
set -euo pipefail
case "${1:---reuse}" in
  --reuse) if docker image inspect axiz/java-build-base:1.1.0 >/dev/null 2>&1; then printf 'Se reutiliza axiz/java-build-base:1.1.0\n'; exit 0; fi ;;
  --rebuild) ;;
  *) printf 'Uso: bash infrastructure/build-base.sh [--reuse|--rebuild]\n' >&2; exit 2 ;;
esac
docker build -t axiz/java-build-base:1.1.0 -f infrastructure/Dockerfile.base .
