#!/usr/bin/env bash
set -euo pipefail

# ─── Helpers ─────────────────────────────────────────────────────────────────
info()  { echo "[INFO]  $*"; }
warn()  { echo "[WARN]  $*"; }
error() { echo "[ERROR] $*" >&2; exit 1; }

# ─── Variables
KUBECTL_INSTALLED=false
KIND_INSTALLED=false
DOCKER_INSTALLED=false
CLUSTER_CREATED=false
HELM_INSTALLED=false
SUBSTRATE_INSTALLED=false
CLUSTER_NAME="default"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CONFIG_FILE="${SCRIPT_DIR}/kind-cluster.yaml"

# ─── Check if OPENAI_API_KEY is set
if [[ -z "${OPENAI_API_KEY:-}" ]]; then
  error "OPENAI_API_KEY environment variable is not set. Please set it before running this script."
else
  info "OPENAI_API_KEY is set."
fi

# ─── Check if kubectl is installed
if ! command -v kubectl &> /dev/null; then
  warn "kubectl is not installed. Please install kubectl to proceed."
else
  kubectl_version_output=$(kubectl version --client | grep "Client Version" | awk '{print $3}')
  KUBECTL_INSTALLED=true
  info "Kubectl ${kubectl_version_output} is installed."
fi

# -- Check if kind is installed
if ! command -v kind &> /dev/null; then
  warn "kind is not installed. Please install kind to proceed."
else
  kind_version_output=$(kind --version)
  KIND_INSTALLED=true
  info "${kind_version_output} is installed."
fi

# -- Check if Docker is installed
if ! command -v docker &> /dev/null; then
  warn "Docker is not installed. Please install Docker to proceed."
else
  docker_version_output=$(docker --version)
  DOCKER_INSTALLED=true
  info "${docker_version_output} is installed."
fi

if ! command -v helm &> /dev/null; then
  warn "Helm is not installed. Please install Helm to proceed."
else
  helm_version_output=$(helm version --short)
  HELM_INSTALLED=true
  info "${helm_version_output} is installed."
fi

# ─── Launch kind cluster if both kind and Docker are installed
if [[ "$KIND_INSTALLED" == true && "$DOCKER_INSTALLED" == true ]]; then
  info "Launching kind cluster..."
  kind create cluster --config "${CONFIG_FILE}" --name "${CLUSTER_NAME}"
  CLUSTER_CREATED=true
  info "Kind cluster launched successfully."
else
  error "Cannot launch kind cluster. Please ensure both kind and Docker are installed."
fi

# ─── Install ArgoCD if kubectl is installed and cluster is created
if [[ "$KUBECTL_INSTALLED" == true && "$CLUSTER_CREATED" == true ]]; then
  info "Installing ArgoCD..."
  kubectl create namespace argocd
  kubectl apply -n argocd --server-side --force-conflicts -f https://raw.githubusercontent.com/argoproj/argo-cd/stable/manifests/install.yaml
  info "ArgoCD installed successfully."
else
  error "Cannot install ArgoCD. Please ensure kubectl is installed and the kind cluster is created."
fi


# ─── Install Substrate if Helm is installed and cluster is created
if [[ "$HELM_INSTALLED" == true && "$CLUSTER_CREATED" == true ]]; then
  info "Installing Substrate with CRDs..."
  helm upgrade --install substrate-crds \
    oci://ghcr.io/kagent-dev/substrate/helm/substrate-crds \
    --version 0.0.9 \
    --namespace ate-system --create-namespace --wait
  info "Substrate CRDs installed successfully."
  info "Installing Substrate..."
  helm upgrade --install substrate \
    oci://ghcr.io/kagent-dev/substrate/helm/substrate \
    --version 0.0.9 \
    --namespace ate-system --wait --timeout 10m
  SUBSTRATE_INSTALLED=true
  info "Substrate installed successfully."
else
  error "Cannot install Substrate. Please ensure Helm is installed and the kind cluster is created."
fi

# ─── Install Kagent if Substrate is installed and cluster is created
if [[ "$SUBSTRATE_INSTALLED" == true ]]; then
  info "Installing Kagent with CRDs..."
  helm install kagent-crds oci://ghcr.io/kagent-dev/kagent/helm/kagent-crds \
      --namespace kagent \
      --version 0.9.9 \
      --create-namespace
  info "Kagent CRDs installed successfully."
  info "Installing Kagent..."
  helm upgrade --install kagent \
      oci://ghcr.io/kagent-dev/kagent/helm/kagent \
      --version 0.9.9 \
      --namespace kagent --timeout 10m --wait \
      --set providers.openAI.apiKey="${OPENAI_API_KEY}" \
      --set providers.default=openAI \
      --set controller.substrate.enabled=true \
      --set controller.substrate.ateApiEndpoint=dns:///api.ate-system.svc:443 \
      --set controller.substrate.ateApiInsecure=true \
      --set substrateWorkerPool.create=true \
      --set substrateWorkerPool.replicas=1 \
      --set substrateWorkerPool.ateomImage=ghcr.io/kagent-dev/substrate/ateom-gvisor:v0.0.6
else
  error "Cannot install Kagent. Please ensure Substrate is installed and the kind cluster is created."
fi