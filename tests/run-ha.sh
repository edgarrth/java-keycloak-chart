#!/usr/bin/env bash
set -euo pipefail
export PATH="$PWD/.tools:$PATH"
export KUBECONFIG="$PWD/.runtime/kubeconfig"
mkdir -p .runtime
umask 077
kubectl -n axiz rollout status statefulset/keycloak --timeout=10m
for pod in keycloak-0 keycloak-1; do
  kubectl -n axiz wait --for=condition=Ready "pod/$pod" --timeout=5m
  kubectl -n axiz get pod "$pod" -o json >".runtime/$pod.json"
  kubectl -n axiz get endpointslice -l "kubernetes.io/service-name=$pod" -o json >".runtime/$pod-endpoints.json"
  python3 - "$pod" <<'PY'
import json
from pathlib import Path
import sys

name = sys.argv[1]
pod = json.loads(Path(f".runtime/{name}.json").read_text())
slices = json.loads(Path(f".runtime/{name}-endpoints.json").read_text())
ready = [e for s in slices["items"] for e in s["endpoints"] if e.get("conditions", {}).get("ready")]
if len(ready) != 1 or ready[0].get("targetRef", {}).get("name") != name:
    raise SystemExit(f"El Service {name} debe apuntar exclusivamente a su Pod disponible")
if pod["status"]["podIP"] not in ready[0]["addresses"]:
    raise SystemExit("No coincide la IP del Pod con su EndpointSlice")
PY
done
# Se verifica que ambos procesos pertenecen a la misma vista Infinispan vigente.
cluster_ready=false
for ((attempt = 0; attempt < 60; attempt++)); do
  joined=0
  for pod in keycloak-0 keycloak-1; do
    kubectl -n axiz logs "$pod" --tail=1000 >".runtime/$pod-cluster.log"
    if python3 - "$pod" <<'PY'
from pathlib import Path
import re
import sys

views = [s for s in Path(f".runtime/{sys.argv[1]}-cluster.log").read_text().splitlines() if "ISPN000094" in s]
if not views or not re.search(r"\([2-8]\)", views[-1]):
    raise SystemExit(1)
if not all(name in views[-1] for name in ("keycloak-0", "keycloak-1")):
    raise SystemExit(1)
PY
    then
      joined=$((joined + 1))
    fi
  done
  if [[ "$joined" == 2 ]]; then
    cluster_ready=true
    break
  fi
  sleep 2
done
[[ "$cluster_ready" == true ]] || {
  printf 'No se confirmó la vista Infinispan compartida por ambas instancias.\n' >&2
  exit 1
}
docker compose --env-file .env -f infrastructure/docker-compose-tests.yml run --rm --no-deps ha-test-runner
