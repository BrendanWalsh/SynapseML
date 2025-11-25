---
description: Check the latest CI build failures for the current branch
---

1. **Identify the current branch**
   ```bash
   git branch --show-current
   ```

2. **Find the latest build ID for this branch**
   Replace `<BRANCH_NAME>` with the output from step 1.
   ```bash
   az pipelines build list --org https://msdata.visualstudio.com --project A365 --definition-ids 17563 --branch refs/heads/<BRANCH_NAME> --top 1 --query "[0].{id:id, buildNumber:buildNumber, status:status, result:result, url:url}"
   ```

3. **Identify failed steps and get Log URLs**
   Replace `<BUILD_ID>` with the ID from step 2.
   ```bash
   az devops invoke --area build --resource timeline --route-parameters project=A365 buildId=<BUILD_ID> --org https://msdata.visualstudio.com --api-version 6.0 --query "records[?result=='failed'].{name:name, logUrl:log.url, error:errorCount}"
   ```

4. **Fetch and Analyze Logs**
   For each failed step, use the `logId` (extracted from the `logUrl`, e.g., `.../logs/123` -> `123`).
   Replace `<LOG_ID>` and `<BUILD_ID>`.
   ```bash
   az devops invoke --area build --resource logs --route-parameters project=A365 buildId=<BUILD_ID> logId=<LOG_ID> --org https://msdata.visualstudio.com --api-version 6.0
   ```

   *Tip: Pipe the output to a file or use `grep` to find "Failed tests" or "Exception".*
