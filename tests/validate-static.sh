#!/usr/bin/env bash
set -euo pipefail
export PATH="$PWD/.tools:$PATH"
find infraestructura datasets tests -name '*.sh' -print0 | xargs -0 -n 1 bash -n
python3 -m py_compile tests/e2e.py
python3 -m json.tool datasets/axiz-realm.json >/dev/null
docker compose --env-file .env -f infraestructura/docker-compose.yml --profile app config --quiet
mkdir -p .runtime
umask 077
set -a
source .env
set +a
python3 infraestructura/helm-values.py
helm lint infraestructura/helm/axiz --kube-version 1.36.4 -f .runtime/helm-values.json
helm template axiz infraestructura/helm/axiz --kube-version 1.36.4 -f .runtime/helm-values.json >/dev/null
