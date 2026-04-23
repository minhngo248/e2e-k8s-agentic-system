#!/usr/bin/env bash
set -euo pipefail

# ─── Helpers ─────────────────────────────────────────────────────────────────
info()  { echo "[INFO]  $*"; }
warn()  { echo "[WARN]  $*"; }
error() { echo "[ERROR] $*" >&2; exit 1; }

# ─── Variables
KIND_INSTALLED=false
DOCKER_INSTALLED=false
CLUSTER_NAME="default"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CONFIG_FILE="${SCRIPT_DIR}/kind-cluster.yaml"

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

# ─── Launch kind cluster if both kind and Docker are installed
if [[ "$KIND_INSTALLED" == true && "$DOCKER_INSTALLED" == true ]]; then
  info "Launching kind cluster..."
  kind create cluster --config "${CONFIG_FILE}" --name "${CLUSTER_NAME}"
  info "Kind cluster launched successfully."
else
  error "Cannot launch kind cluster. Please ensure both kind and Docker are installed."
fi