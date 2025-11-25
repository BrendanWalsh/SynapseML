# CI Failure Analysis Protocol

**Goal:** Analyze CI failures for the current branch using Azure CLI, bypassing unreliable Test API commands.

## 1. Identify the Latest Build
Find the latest build ID for the current branch.

```bash
# Get current branch
BRANCH=$(git branch --show-current)

# Get latest build ID
az pipelines build list --org https://msdata.visualstudio.com --project A365 --definition-ids 17563 --branch refs/heads/$BRANCH --top 1 --query "[0].{id:id, buildNumber:buildNumber, result:result}"
```

## 2. Scope Failures (High-Level)
Identify which modules failed by listing failed **Jobs**.

```bash
# Replace <BUILD_ID> with the ID from Step 1
az devops invoke --area build --resource timeline --route-parameters project=A365 buildId=<BUILD_ID> --org https://msdata.visualstudio.com --api-version 6.0 --query "records[?type=='Job' && result=='failed'].{name:name}"
```

## 3. Analyze Logs (Deep Dive)
Retrieve specific failure details (exceptions, stack traces) from the logs.

### 3.1 Get Log URLs
Find the Log URLs for all failed steps.

```bash
# Replace <BUILD_ID>
az devops invoke --area build --resource timeline --route-parameters project=A365 buildId=<BUILD_ID> --org https://msdata.visualstudio.com --api-version 6.0 --query "records[?result=='failed'].{name:name, logUrl:log.url}"
```

### 3.2 Fetch Log Content
Extract the `logId` from the URL (e.g., `.../logs/123` -> `123`) and fetch the content.

```bash
# Replace <BUILD_ID> and <LOG_ID>
az devops invoke --area build --resource logs --route-parameters project=A365 buildId=<BUILD_ID> logId=<LOG_ID> --org https://msdata.visualstudio.com --api-version 6.0
```
