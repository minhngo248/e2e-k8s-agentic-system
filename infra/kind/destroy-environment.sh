#!/usr/bin/env bash
set -euo pipefail

# ─── Helpers ─────────────────────────────────────────────────────────────────
info()  { echo "[INFO]  $*"; }
warn()  { echo "[WARN]  $*"; }
error() { echo "[ERROR] $*" >&2; exit 1; }

# ─── Variables
CLUSTER_NAME="default"

# ─── Destroy kind cluster
if kind get clusters | grep -q "${CLUSTER_NAME}"; then
  info "Destroying kind cluster '${CLUSTER_NAME}'..."
  kind delete cluster --name "${CLUSTER_NAME}"
  info "Kind cluster '${CLUSTER_NAME}' destroyed successfully."
else
  warn "Kind cluster '${CLUSTER_NAME}' does not exist. Nothing to destroy."
fi