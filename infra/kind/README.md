# Agent substrate CRDs
```bash
helm upgrade --install substrate-crds \
  oci://ghcr.io/kagent-dev/substrate/helm/substrate-crds \
  --version 0.0.9 \
  --namespace ate-system --create-namespace --wait
  
helm upgrade --install substrate \
  oci://ghcr.io/kagent-dev/substrate/helm/substrate \
  --version 0.0.9 \
  --namespace ate-system --wait --timeout 10m
```