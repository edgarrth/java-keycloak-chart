#!/usr/bin/env bash
set -euo pipefail
if [[ ! -f .env.example || ! -f infrastructure/datasets/postman/axiz-local.postman_environment.json ]]; then
  printf 'Se ejecuta este script desde la raíz del proyecto.\n' >&2
  exit 1
fi
case "${1:-64}" in
*[!0-9]* | '')
  printf 'Se indica una cantidad entre 1 y 1024.\n' >&2
  exit 2
  ;;
esac
if ((${1:-64} < 1 || ${1:-64} > 1024)); then
  printf 'Se indica una cantidad entre 1 y 1024.\n' >&2
  exit 2
fi
config_file=.env.example
if [[ -f .env ]]; then
  config_file=.env
fi
set -a
source "$config_file"
set +a
mkdir -p .runtime
umask 077
python3 - "${1:-64}" <<'PY'
import base64
import hashlib
import json
import os
from pathlib import Path
import secrets
import sys


def base64url(value):
    return base64.urlsafe_b64encode(value).rstrip(b"=").decode("ascii")


source = Path("infrastructure/datasets/postman/axiz-local.postman_environment.json")
environment = json.loads(source.read_text())
overrides = {
    "keycloak_url": "KEYCLOAK_PUBLIC_URL",
    "api_url": "API_URL",
    "dni_password": "DNI_PASSWORD",
    "phone_password": "PHONE_DEMO_PASSWORD",
    "password_client_secret": "PASSWORD_CLIENT_SECRET",
    "phone_client_secret": "PHONE_CLIENT_SECRET",
    "admin_username": "KC_BOOTSTRAP_ADMIN_USERNAME",
    "admin_password": "KC_BOOTSTRAP_ADMIN_PASSWORD",
}
for entry in environment["values"]:
    if entry["key"] in overrides:
        entry["value"] = os.environ.get(overrides[entry["key"]], entry["value"])
api_url = next(v["value"] for v in environment["values"] if v["key"] == "api_url")
for entry in environment["values"]:
    if entry["key"] == "redirect_uri":
        entry["value"] = api_url.rstrip("/") + "/callback-test"
pool = []
for _ in range(int(sys.argv[1])):
    verifier = secrets.token_urlsafe(32)
    pool.append({
        "verifier": verifier,
        "challenge": base64url(hashlib.sha256(verifier.encode("ascii")).digest()),
        "state": secrets.token_urlsafe(24),
        "nonce": secrets.token_urlsafe(24),
    })
environment["values"] = [v for v in environment["values"] if v["key"] != "pkce_pool"]
environment["values"].append({
    "key": "pkce_pool",
    "value": json.dumps(pool, separators=(",", ":")),
    "enabled": True,
    "type": "secret",
})
environment["name"] = "Axiz local - PKCE compatible con Postman 10"
destination = Path(".runtime/axiz-local.postman_environment.json")
destination.write_text(json.dumps(environment, ensure_ascii=False, indent=2) + "\n")
destination.chmod(0o600)
print(f"Se generaron {len(pool)} conjuntos PKCE seguros en {destination}.")
PY
