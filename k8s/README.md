1. Kind cluster
Create garage
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

Create `postgres` secret, `tourist-api` secret, and `agent` secret before 
deploying the application using Kustomize.
```bash
k -n default create secret generic postgres --from-env-file=k8s/.env.postgres
k -n default create secret generic tourist-api --from-env-file=k8s/.env.touristapi
k -n default create secret generic agent --from-env-file=k8s/.env.agent
```

Then, deploy the application using Kustomize:
```bash
k apply -k k8s/overlays/kind
```