#!/usr/bin/env bash
set -euo pipefail

# ─── Helpers ─────────────────────────────────────────────────────────────────
info()  { echo "[INFO]  $*"; }
warn()  { echo "[WARN]  $*"; }
error() { echo "[ERROR] $*" >&2; exit 1; }

# ─── Variables
KUBECTL_INSTALLED=false
HELM_INSTALLED=false
SUBSTRATE_INSTALLED=false
OIDC_ISSUER_URL=""

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
  OIDC_ISSUER_URL=$(kubectl get --raw /.well-known/openid-configuration | jq -r '.issuer')
  KUBECTL_INSTALLED=true
  info "Kubectl ${kubectl_version_output} is installed."
fi

if ! command -v helm &> /dev/null; then
  warn "Helm is not installed. Please install Helm to proceed."
else
  helm_version_output=$(helm version --short)
  HELM_INSTALLED=true
  info "${helm_version_output} is installed."
fi

# ─── Install ArgoCD if kubectl is installed
if [[ "$KUBECTL_INSTALLED" == true ]]; then
  info "Installing ArgoCD..."
  kubectl create namespace argocd
  kubectl apply -n argocd --server-side --force-conflicts -f https://raw.githubusercontent.com/argoproj/argo-cd/stable/manifests/install.yaml
  info "ArgoCD installed successfully."
else
  error "Cannot install ArgoCD. Please ensure kubectl is installed."
fi


# ─── Install Substrate if Helm is installed
if [[ "$HELM_INSTALLED" == true ]]; then
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
    --set auth.jwt.issuer="${OIDC_ISSUER_URL}" \
    --namespace ate-system --wait --timeout 10m
  SUBSTRATE_INSTALLED=true
  info "Substrate installed successfully."
else
  error "Cannot install Substrate. Please ensure Helm is installed."
fi

# ─── Install Kagent if Substrate is installed
if [[ "$SUBSTRATE_INSTALLED" == true ]]; then
  info "Installing Kagent with CRDs..."
  helm upgrade --install kagent-crds oci://ghcr.io/kagent-dev/kagent/helm/kagent-crds \
      --namespace kagent \
      --version 0.9.9 \
      --create-namespace
  info "Kagent CRDs installed successfully."
  info "Installing Kagent..."
  helm upgrade --install kagent \
      oci://ghcr.io/kagent-dev/kagent/helm/kagent \
      --version 0.9.9 \
      --namespace kagent --timeout 10m --wait \
      --set registry=ghcr.io \
      --set providers.openAI.apiKey="${OPENAI_API_KEY}" \
      --set providers.default=openAI \
      --set controller.substrate.enabled=true \
      --set controller.substrate.ateApiEndpoint=dns:///api.ate-system.svc:443 \
      --set controller.substrate.ateApiInsecure=false \
      --set substrateWorkerPool.create=true \
      --set substrateWorkerPool.replicas=3 \
      --set substrateWorkerPool.ateomImage=ghcr.io/kagent-dev/substrate/ateom-gvisor:v0.0.9
else
  error "Cannot install Kagent. Please ensure Substrate is installed."
fi