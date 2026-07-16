# Create an environment in Kind cluster
## Prerequisites
- Make sure that `OPENAI_API_KEY` is set in your environment.
- kubectl is installed.
- Docker is installed.
- Kind is installed.
- Helm is installed.
```bash
export OPENAI_API_KEY=sk-...
```

## Initialize the cluster
- Run the following command to initialize the cluster.
```bash
./init-environment.sh
```
- Kind cluster named `default` will be created.
- ArgoCD will be installed in the cluster.
- KAgent Substrate will be installed in the cluster.
- KAgent will be installed to the cluster.