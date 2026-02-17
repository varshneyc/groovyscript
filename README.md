# Azure Container App Deployment – Jenkins Pipeline

General Groovy pipeline for deploying to **Azure Container Apps**, with the pipeline script loaded from **GitHub (SCM)**. You pass **environment** and other **variables** as Jenkins parameters or via config.

## Contents

| File | Purpose |
|------|--------|
| `Jenkinsfile` | Main pipeline: GitHub SCM, parameters, Azure login, optional build, deploy |
| `scripts/azureContainerAppDeploy.groovy` | Reusable deploy step (optional) |
| `scripts/env-config.example.groovy` | Example per-environment config (optional) |

---

## 1. Jenkins job setup (Pipeline from SCM)

1. **New Item** → **Pipeline**.
2. **Pipeline** section:
   - **Definition**: **Pipeline script from SCM**
   - **SCM**: **Git**
   - **Repository URL**: your GitHub repo (e.g. `https://github.com/your-org/your-repo.git`)
   - **Branch**: `*/main` (or your default branch)
   - **Script Path**: `Jenkinsfile`
3. Save. The first run will create the parameters; fill defaults if you want.

---

## 2. Parameters and variables

### Required parameters

| Parameter | Description |
|-----------|-------------|
| `ENVIRONMENT` | Target environment: `dev`, `staging`, or `prod` (used in logic/labels; Azure resources are still chosen by the other params). |
| `AZURE_SUBSCRIPTION_ID` | Azure subscription ID. |
| `AZURE_RESOURCE_GROUP` | Resource group that contains the Container App. |
| `CONTAINER_APP_NAME` | Name of the Azure Container App. |
| `CONTAINER_APP_ENVIRONMENT` | Container Apps environment name (e.g. `myapp-env`). |
| `ACR_NAME` | Azure Container Registry name (without `.azurecr.io`). |
| `IMAGE_NAME` | Container image name (e.g. `myapp`). |
| `AZURE_CREDENTIALS_ID` | Jenkins credential ID for Azure Service Principal. |

### Optional parameters

| Parameter | Default | Description |
|-----------|---------|-------------|
| `IMAGE_TAG` | `latest` | Image tag. If you build in pipeline and keep default, build number is used when not skipping build. |
| `REPLICAS` | `1` | Min (and max) replicas. |
| `CPU` | `0.5` | CPU cores per replica (e.g. `0.5`, `1`). |
| `MEMORY` | `1Gi` | Memory per replica (e.g. `1Gi`, `2Gi`). |
| `ENV_VARS` | (empty) | Env vars for the container, one per line: `KEY1=value1`. |
| `SKIP_BUILD` | `true` | If `true`, only deploy existing image; if `false`, build from repo and push to ACR. |

---

## 3. Passing environment and variables

- **Environment**: Set **ENVIRONMENT** when you run the build (`dev` / `staging` / `prod`). Use it in your process (e.g. to pick a config or to pass into scripts).
- **Variables**:  
  - Set **ENV_VARS** in the job (multiline: `KEY1=value1`, etc.).  
  - Or in Jenkins: use **Inject environment variables** or a config file and map them into **ENV_VARS** or into the deploy step.

Example **ENV_VARS** in the job:

```text
ASPNETCORE_ENVIRONMENT=Production
APP_SETTINGS__SomeKey=value
LOG_LEVEL=Information
```

---

## 4. Azure credentials in Jenkins

1. **Jenkins** → **Manage Jenkins** → **Credentials** → **Add**.
2. Kind: **Microsoft Azure Service Principal**.
3. Set:
   - **Subscription ID**
   - **Client ID**
   - **Tenant ID**
   - **Client Secret**
4. Set **ID** to something like `azure-sp-containerapp` and use this ID as **AZURE_CREDENTIALS_ID** in the pipeline.

Required plugins (typical):

- **Pipeline**
- **Azure Credentials** (or **Azure Service Principal**)
- **Credentials Binding**

Agent must have **Azure CLI** installed (`az` in `PATH`). Install extension:

```bash
az extension add --name containerapp --upgrade
```

---

## 5. Pipeline flow

1. **Checkout** – SCM (GitHub) checkout.
2. **Validate Parameters** – Ensures required parameters are set.
3. **Azure Login** – Uses `AZURE_CREDENTIALS_ID` and `AZURE_SUBSCRIPTION_ID`.
4. **Build & Push** (only if `SKIP_BUILD == false`) – `docker build`, push to ACR.
5. **Deploy** – `az containerapp update` with image, replicas, CPU, memory, and env vars.

---

## 6. Using the reusable deploy script (optional)

From a **Scripted Pipeline** or from a `script {}` block you can load and call the shared script:

```groovy
// After checkout
def deploy = load 'scripts/azureContainerAppDeploy.groovy'
deploy(
    resourceGroup    : params.AZURE_RESOURCE_GROUP,
    containerAppName  : params.CONTAINER_APP_NAME,
    image            : "${params.ACR_NAME}.azurecr.io/${params.IMAGE_NAME}:${params.IMAGE_TAG}",
    minReplicas      : params.REPLICAS,
    maxReplicas      : params.REPLICAS,
    cpu              : params.CPU,
    memory           : params.MEMORY,
    subscriptionId   : params.AZURE_SUBSCRIPTION_ID,
    envVars          : ['KEY1=value1', 'KEY2=value2']  // or Map
)
```

You can combine this with an env-specific config (e.g. `scripts/env-config.example.groovy`) and pass the map returned for `params.ENVIRONMENT` into `deploy(...)`.

---

## 7. Creating the Container App (one-time)

If the Container App does not exist yet, create it once (e.g. from your machine or a one-off job):

```bash
az containerapp create \
  --name <CONTAINER_APP_NAME> \
  --resource-group <RESOURCE_GROUP> \
  --environment <CONTAINER_APP_ENVIRONMENT> \
  --image <ACR>.azurecr.io/<IMAGE>:latest \
  --registry-server <ACR>.azurecr.io \
  --target-port 80 \
  --ingress external
```

After that, this pipeline only needs to run **Update** (as in the main `Jenkinsfile`).

---

## 8. Summary

- **Script from GitHub**: Use **Pipeline script from SCM** and set **Script Path** to `Jenkinsfile`.
- **Environments**: Pass `ENVIRONMENT` (e.g. dev/staging/prod) and use it to choose config or labels.
- **Variables**: Pass container env vars via **ENV_VARS** (and optionally via env config or injectors).
- **Credentials**: Store Azure Service Principal in Jenkins and set **AZURE_CREDENTIALS_ID** and **AZURE_SUBSCRIPTION_ID** (and other required params) when running the job.
