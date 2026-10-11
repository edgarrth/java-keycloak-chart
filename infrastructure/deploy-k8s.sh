#!/usr/bin/env bash
set -euo pipefail
export PATH="$PWD/.tools:$PATH"
set -a
source .env
set +a
case "${1:---build}" in
--build | --no-build) ;;
*)
  printf 'Uso: bash infrastructure/deploy-k8s.sh [--build|--no-build]\n' >&2
  exit 2
  ;;
esac
compose=(docker compose --env-file .env -f infrastructure/docker-compose.yml)
for service in postgres-keycloak postgres-external; do
  container=$("${compose[@]}" ps -q "$service")
  if [[ -z "$container" ]] || [[ $(docker inspect --format '{{.State.Health.Status}}' "$container") != healthy ]]; then
    printf 'Se requiere %s saludable. Se utiliza bash infrastructure/start-all.sh para el arranque completo.\n' "$service" >&2
    exit 1
  fi
done
bash infrastructure/provision-k8s.sh
export KUBECONFIG="$PWD/.runtime/kubeconfig"
# Se conectan las dos instancias externas al puente Docker, sin crear workloads PostgreSQL.
for service in postgres-keycloak postgres-external; do
  container=$("${compose[@]}" ps -q "$service")
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
  docker compose --env-file .env -f infrastructure/docker-compose.yml build keycloak
fi
export KEYCLOAK_IMAGE_REVISION
KEYCLOAK_IMAGE_REVISION=$(docker image inspect --format '{{.Id}}' axiz/keycloak:1.1.0)
kind load docker-image axiz/keycloak:1.1.0 --name axiz
helm upgrade --install eg oci://docker.io/envoyproxy/gateway-helm --version v1.9.2 --namespace envoy-gateway-system --create-namespace --wait --timeout 10m
umask 077
python3 infrastructure/helm-values.py
helm upgrade --install axiz infrastructure/helm/axiz --namespace axiz --create-namespace -f .runtime/helm-values.json --wait --timeout 15m
kubectl -n axiz rollout status statefulset/keycloak --timeout=10m
kubectl -n axiz wait gateway/axiz-keycloak --for=condition=Programmed --timeout=5m
"${compose[@]}" run --rm seed-keycloak
