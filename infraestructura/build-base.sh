#!/usr/bin/env bash
set -euo pipefail
docker build -t axiz/java-build-base:1.0.0 -f infraestructura/Dockerfile.base .
