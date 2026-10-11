#!/usr/bin/env bash
set -euo pipefail
export PATH="$PWD/.tools:$PATH"
mkdir -p .runtime
umask 077
for tool in kind helm kubectl; do
  if ! command -v "$tool" >/dev/null; then
    bash infrastructure/install-k8s-tools.sh
    break
  fi
done
if ! kind get clusters | grep -qx axiz; then
  kind create cluster --config infrastructure/kind.yaml --kubeconfig .runtime/kubeconfig --image kindest/node:v1.36.4@sha256:099e049362a1526b2db71494e1947aae99bd16290d7c895f2b7ea312e3cbfaed
fi
kind get kubeconfig --name axiz >.runtime/kubeconfig
chmod 600 .runtime/kubeconfig
kubectl --kubeconfig .runtime/kubeconfig wait node --all --for=condition=Ready --timeout=5m
