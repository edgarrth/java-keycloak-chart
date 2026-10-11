#!/usr/bin/env bash
set -euo pipefail
export PATH="$PWD/.tools:$PATH"
mode=--build
run_tests=false
for argument in "$@"; do
  case "$argument" in
  --build | --no-build) mode="$argument" ;;
  --test) run_tests=true ;;
  *)
    printf 'Uso: bash infrastructure/start-all.sh [--build|--no-build] [--test]\n' >&2
    exit 2
    ;;
  esac
done
[[ -f pom.xml && -d infrastructure ]] || {
  printf 'Se ejecuta desde la raíz del proyecto.\n' >&2
  exit 1
}
for tool in docker python3 curl; do
  command -v "$tool" >/dev/null || {
    printf 'Se requiere %s en el host.\n' "$tool" >&2
    exit 1
  }
done
if [[ ! -f .env ]]; then
  cp .env.example .env
  chmod 600 .env
  printf 'Se creó .env con las credenciales locales de demostración.\n'
fi
set -a
source .env
set +a
API_URL=${API_URL:-http://localhost:8090}
docker info >/dev/null
compose=(docker compose --env-file .env -f infrastructure/docker-compose.yml)
"${compose[@]}" --profile app config --quiet
if [[ "$mode" == --no-build ]]; then
  for image in axiz/keycloak:1.1.0 axiz/payments:1.1.0; do
    docker image inspect "$image" >/dev/null 2>&1 || {
      printf 'No existe %s. Se utiliza --build en el primer arranque.\n' "$image" >&2
      exit 1
    }
  done
fi
mkdir -p .runtime
chmod 700 .runtime
umask 077
# Se liberan puertos del modo Compose sin eliminar volúmenes ni otras aplicaciones.
"${compose[@]}" --profile app --profile tasks stop keycloak payments seed-keycloak postgres-keycloak
bash infrastructure/provision-k8s.sh
database_pid=""
payments_pid=""
cleanup() {
  status=$?
  trap - EXIT
  kubectl --kubeconfig .runtime/kubeconfig -n axiz scale statefulset/keycloak --replicas=0 >/dev/null 2>&1 || true
  kubectl --kubeconfig .runtime/kubeconfig -n axiz wait --for=delete pod -l app=keycloak --timeout=60s >/dev/null 2>&1 || true
  "${compose[@]}" --profile app stop payments postgres-keycloak postgres-external >/dev/null 2>&1 || true
  for pid in "$database_pid" "$payments_pid"; do
    if [[ -n "$pid" ]]; then kill -TERM "$pid" 2>/dev/null || true; fi
  done
  wait 2>/dev/null || true
  printf 'Se detuvo Compose; el clúster, el chart y sus datos permanecen disponibles.\n'
  return "$status"
}
trap cleanup EXIT
trap 'exit 130' INT
trap 'exit 143' TERM
"${compose[@]}" up --no-build postgres-keycloak postgres-external &
database_pid=$!
database_ready=false
for attempt in $(seq 1 120); do
  kill -0 "$database_pid" || {
    printf 'Se detuvo el proceso de las bases.\n' >&2
    exit 1
  }
  healthy=0
  for service in postgres-keycloak postgres-external; do
    container=$("${compose[@]}" ps -q "$service")
    if [[ -n "$container" ]] && [[ $(docker inspect --format '{{.State.Health.Status}}' "$container") == healthy ]]; then
      healthy=$((healthy + 1))
    fi
  done
  if [[ "$healthy" == 2 ]]; then
    database_ready=true
    break
  fi
  sleep 2
done
[[ "$database_ready" == true ]] || {
  printf 'Las dos instancias PostgreSQL no quedaron disponibles en cuatro minutos.\n' >&2
  exit 1
}
bash infrastructure/deploy-k8s.sh "$mode"
if [[ "$mode" == --build ]]; then "${compose[@]}" --profile app build payments; fi
"${compose[@]}" --profile app up --no-build payments &
payments_pid=$!
api_ready=false
for attempt in $(seq 1 120); do
  kill -0 "$payments_pid" || {
    printf 'Se detuvo el servicio de pagos.\n' >&2
    exit 1
  }
  if curl -fsS "$API_URL/ready" >/dev/null 2>&1; then
    api_ready=true
    break
  fi
  sleep 2
done
[[ "$api_ready" == true ]] || {
  printf 'La API no quedó disponible en cuatro minutos.\n' >&2
  exit 1
}
bash infrastructure/routes.sh
if [[ "$run_tests" == true ]]; then
  docker compose --env-file .env -f infrastructure/docker-compose-tests.yml run --rm test-runner
  bash tests/run-ha.sh
fi
printf 'Se completó el arranque: Keycloak en Helm; dos instancias PostgreSQL y pagos en Compose. Se detiene el modo local con Ctrl+C.\n'
wait "$payments_pid"
