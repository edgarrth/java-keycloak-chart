#!/usr/bin/env bash
set -euo pipefail
set -a
source .env
set +a
mvn -B -ntp package
bash infrastructure/routes.sh
java -jar payment-service/target/payment-service-1.1.0-exec.jar
