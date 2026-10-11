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
python3 - <<'PY'
import json,os,pathlib
names={'postgres':'POSTGRES_PASSWORD','keycloak':'KC_DB_PASSWORD','phone':'PHONE_DB_PASSWORD','payment':'PAYMENT_DB_PASSWORD','adminUser':'KC_BOOTSTRAP_ADMIN_USERNAME','adminPassword':'KC_BOOTSTRAP_ADMIN_PASSWORD','dniPassword':'DNI_PASSWORD','passwordClient':'PASSWORD_CLIENT_SECRET','phoneClient':'PHONE_CLIENT_SECRET'}
pathlib.Path('.runtime/helm-values.json').write_text(json.dumps({'credentials':{k:os.environ[v] for k,v in names.items()}}))
PY
helm lint infraestructura/helm/axiz -f .runtime/helm-values.json
helm template axiz infraestructura/helm/axiz -f .runtime/helm-values.json >/dev/null
