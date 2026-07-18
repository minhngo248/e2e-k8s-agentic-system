1. Create garage
- Only for Kind cluster
```bash
helm -n garage install garage garage/ -f values.override.yaml --create-namespace

# Create cluster layout
kubectl -n garage exec -it sts/garage -- ./garage layout assign   -z kind -c 1024  <ID Garage>

# Create API Key
kubectl -n garage exec -it sts/garage -- ./garage key create tourist-api-key

# Create bucket
kubectl -n garage exec -it sts/garage -- ./garage bucket create tourist-images

# Add bucket policy to API Key
kubectl -n garage exec -it sts/garage -- ./garage bucket allow   --read --write --key tourist-api-key   tourist-images
```

2. Gateway API and agentgateway
- For Kind cluster and GKE cluster
```bash
# Install Gateway API
kkubectl apply --server-side --force-conflicts -f https://github.com/kubernetes-sigs/gateway-api/releases/download/v1.5.0/standard-install.yaml

# Install agentgateway CRDs
helm upgrade -i agentgateway-crds oci://cr.agentgateway.dev/charts/agentgateway-crds \
--create-namespace --namespace agentgateway-system \
--version v1.3.1 \
--set controller.image.pullPolicy=Always

# Install agentgateway control plane
helm upgrade -i agentgateway oci://cr.agentgateway.dev/charts/agentgateway \
  --namespace agentgateway-system \
  --version v1.3.1 \
  --set controller.image.pullPolicy=Always \
  --wait
```

- Install `cloud-provider-kind` inside your machine
```bash
go install sigs.k8s.io/cloud-provider-kind@latest
sudo install ~/go/bin/cloud-provider-kind /usr/local/bin

# Then run
cloud-provider-kind --gateway-channel standard
```

- Install Gateway referencing to `cloud-provider-kind` GatewayClass
```bash
kubectl apply -f- <<EOF
apiVersion: gateway.networking.k8s.io/v1
kind: Gateway
metadata:
  name: agentgateway-proxy
  namespace: agentgateway-system
spec:
  gatewayClassName: agentgateway # cloud-provider-kind
  listeners:
  - protocol: HTTP
    port: 80
    name: http
    allowedRoutes:
      namespaces:
        from: All
EOF
```

3. Deploy applications
- For Kind cluster and GKE cluster
- Create `postgres` secret, `tourist-api` secret, and `agent` secret before 
deploying the application using Kustomize.
```bash
# Create secrets for Kind cluster
k -n default create secret generic postgres --from-env-file=k8s/overlays/kind/.env.postgres
k -n default create secret generic tourist-api --from-env-file=k8s/overlays/kind/.env.touristapi
k -n kagent create secret generic agent --from-env-file=k8s/overlays/kind/.env.agent

# Create secrets for GKE cluster
k -n default create secret generic tourist-api --from-env-file=k8s/overlays/gke/.env.touristapi
k -n kagent create secret generic agent --from-env-file=k8s/overlays/gke/.env.agent
```

Then, deploy the application using Kustomize:
```bash
k apply -k k8s/overlays/kind
```