#!/usr/bin/env bash
set -euo pipefail
export PATH="$PWD/.tools:$PATH"
find infrastructure tests -name '*.sh' -print0 | xargs -0 -n 1 bash -n
python3 -m py_compile tests/e2e.py
python3 -m json.tool infrastructure/datasets/axiz-realm.json >/dev/null
python3 -m json.tool infrastructure/datasets/postman/axiz-keycloak.postman_collection.json >/dev/null
python3 -m json.tool infrastructure/datasets/postman/axiz-local.postman_environment.json >/dev/null
docker compose --env-file .env -f infrastructure/docker-compose.yml --profile app --profile tasks config --quiet
docker compose --env-file .env -f infrastructure/docker-compose-tests.yml config --quiet
mkdir -p .runtime
umask 077
set -a
source .env
set +a
python3 infrastructure/helm-values.py
helm lint infrastructure/helm/axiz --kube-version 1.36.4 -f .runtime/helm-values.json
helm template axiz infrastructure/helm/axiz --kube-version 1.36.4 -f .runtime/helm-values.json >/dev/null
