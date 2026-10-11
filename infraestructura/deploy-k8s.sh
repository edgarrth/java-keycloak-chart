#!/usr/bin/env bash
set -euo pipefail
export PATH="$PWD/.tools:$PATH"
set -a
source .env
set +a
if ! command -v kind >/dev/null || ! command -v helm >/dev/null || ! command -v kubectl >/dev/null; then
  bash infraestructura/install-k8s-tools.sh
fi
if ! kind get clusters | grep -qx axiz; then
  kind create cluster --config infraestructura/kind.yaml --image kindest/node:v1.37.0@sha256:a1ed56cfb0e7b93589bdf97c8cd566405a265939e3620fc4f5de89adff580ae5
fi
kubectl config use-context kind-axiz
if [[ "${1:---build}" == --build ]]; then
  bash infraestructura/build-base.sh
  docker compose --env-file .env -f infraestructura/docker-compose.yml --profile app build keycloak payments
fi
kind load docker-image axiz/keycloak:1.0.0 axiz/payments:1.0.0 --name axiz
mkdir -p .runtime infraestructura/helm/axiz/files
cp datasets/axiz-realm.json datasets/phone-seed.sql infraestructura/postgres-init.sh infraestructura/helm/axiz/files/
umask 077
python3 - <<'PYTHON'
import json,os,pathlib
names={'postgres':'POSTGRES_PASSWORD','keycloak':'KC_DB_PASSWORD','phone':'PHONE_DB_PASSWORD','payment':'PAYMENT_DB_PASSWORD','adminUser':'KC_BOOTSTRAP_ADMIN_USERNAME','adminPassword':'KC_BOOTSTRAP_ADMIN_PASSWORD','dniPassword':'DNI_PASSWORD','passwordClient':'PASSWORD_CLIENT_SECRET','phoneClient':'PHONE_CLIENT_SECRET'}
pathlib.Path('.runtime/helm-values.json').write_text(json.dumps({'publicUrl':os.environ['KEYCLOAK_PUBLIC_URL'],'credentials':{k:os.environ[v] for k,v in names.items()}}))
PYTHON
helm upgrade --install axiz infraestructura/helm/axiz --namespace axiz --create-namespace -f .runtime/helm-values.json --wait --timeout 15m
kubectl -n axiz rollout status statefulset/keycloak --timeout=10m
kubectl -n axiz rollout status deployment/payments --timeout=5m
bash infraestructura/routes.sh
