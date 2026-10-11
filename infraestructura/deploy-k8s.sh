#!/usr/bin/env bash
set -euo pipefail
export PATH="$PWD/.tools:$PATH"
set -a
source .env
set +a
case "${1:---build}" in --build|--no-build) ;; *) printf 'Uso: bash infraestructura/deploy-k8s.sh [--build|--no-build]\n' >&2; exit 2;; esac
for service in postgres-keycloak postgres-external; do
  container=$(docker compose --env-file .env -f infraestructura/docker-compose.yml ps -q "$service")
  if [[ -z "$container" ]] || [[ $(docker inspect --format '{{.State.Health.Status}}' "$container") != healthy ]]; then
    printf 'Se requiere %s en ejecución y saludable. Se inicia con el comando de bases del README.\n' "$service" >&2
    exit 1
  fi
done
if ! command -v kind >/dev/null || ! command -v helm >/dev/null || ! command -v kubectl >/dev/null; then
  bash infraestructura/install-k8s-tools.sh
fi
if ! kind get clusters | grep -qx axiz; then
  kind create cluster --config infraestructura/kind.yaml --image kindest/node:v1.36.4@sha256:099e049362a1526b2db71494e1947aae99bd16290d7c895f2b7ea312e3cbfaed
fi
kubectl config use-context kind-axiz
# Se conectan contenedores externos al puente Docker del clúster, sin crear workloads PostgreSQL.
for service in postgres-keycloak postgres-external; do
  container=$(docker compose --env-file .env -f infraestructura/docker-compose.yml ps -q "$service")
  if [[ -z $(docker inspect --format '{{with index .NetworkSettings.Networks "kind"}}{{.IPAddress}}{{end}}' "$container") ]]; then
    docker network connect kind "$container"
  fi
  address=$(docker inspect --format '{{with index .NetworkSettings.Networks "kind"}}{{.IPAddress}}{{end}}' "$container")
  if [[ "$service" == postgres-keycloak ]]; then
    export K8S_KEYCLOAK_DB_URL="jdbc:postgresql://$address:5432/keycloak"
  else
    export K8S_PHONE_DB_URL="jdbc:postgresql://$address:5432/phone_identity"
  fi
done
if [[ "${1:---build}" == --build ]]; then
  bash infraestructura/build-base.sh
  docker compose --env-file .env -f infraestructura/docker-compose.yml build keycloak
fi
kind load docker-image axiz/keycloak:1.1.0 --name axiz
helm upgrade --install eg oci://docker.io/envoyproxy/gateway-helm --version v1.9.2 --namespace envoy-gateway-system --create-namespace --wait --timeout 10m
mkdir -p .runtime
umask 077
python3 infraestructura/helm-values.py
helm upgrade --install axiz infraestructura/helm/axiz --namespace axiz --create-namespace -f .runtime/helm-values.json --wait --timeout 15m
kubectl -n axiz rollout status statefulset/keycloak --timeout=10m
kubectl -n axiz wait gateway/axiz-keycloak --for=condition=Programmed --timeout=5m
KEYCLOAK_INTERNAL_URL="$KEYCLOAK_PUBLIC_URL" python3 infraestructura/seed-keycloak.py
bash infraestructura/routes.sh
