#!/usr/bin/env bash
set -euo pipefail
set -a
source .env
set +a
mvn -B -ntp package
printf 'Portal: http://localhost:%s\nAPI: http://localhost:%s/api/payments\nKeycloak: %s\n' "$SERVER_PORT" "$SERVER_PORT" "$KEYCLOAK_PUBLIC_URL"
java -jar payment-service/target/payment-service-1.0.0-exec.jar
