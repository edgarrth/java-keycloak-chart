#!/usr/bin/env bash
set -euo pipefail
mkdir -p .tools
arch=$(uname -m)
case "$arch" in x86_64) arch=amd64;; aarch64) arch=arm64;; *) printf 'Arquitectura no compatible\n' >&2; exit 1;; esac
curl -fsSL "https://kind.sigs.k8s.io/dl/v0.33.0/kind-linux-$arch" -o .tools/kind
curl -fsSL "https://get.helm.sh/helm-v4.3.0-linux-$arch.tar.gz" -o .tools/helm.tar.gz
curl -fsSL "https://get.helm.sh/helm-v4.3.0-linux-$arch.tar.gz.sha256sum" -o .tools/helm.sha256sum
printf '%s  .tools/helm.tar.gz\n' "$(awk '{print $1}' .tools/helm.sha256sum)" | sha256sum --check
curl -fsSL "https://github.com/kubernetes-sigs/kind/releases/download/v0.33.0/kind-linux-$arch.sha256sum" -o .tools/kind.sha256sum
printf '%s  .tools/kind\n' "$(awk '{print $1}' .tools/kind.sha256sum)" | sha256sum --check
tar -xzf .tools/helm.tar.gz -C .tools
mv ".tools/linux-$arch/helm" .tools/helm
curl -fsSL "https://dl.k8s.io/release/v1.37.0/bin/linux/$arch/kubectl" -o .tools/kubectl
curl -fsSL "https://dl.k8s.io/release/v1.37.0/bin/linux/$arch/kubectl.sha256" -o .tools/kubectl.sha256
printf '%s  .tools/kubectl\n' "$(cat .tools/kubectl.sha256)" | sha256sum --check
chmod +x .tools/kind .tools/helm .tools/kubectl
printf 'Se instalaron herramientas en .tools.\n'
