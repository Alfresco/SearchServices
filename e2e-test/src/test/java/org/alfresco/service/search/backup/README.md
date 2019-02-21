# About
This suite(s) will test the backup process of SearchService/InsightEngine

# Prerequisites

a) `docker-compose.backup.yml`
You need to execute the "start-backup" tasks from the `BuildScripts/qa` folder
That command will provision some docker containers with:
* `alfresco` service having 'remoteBackupLocation' set for `archive` and `alfresco` cores
* `search` service that will automatically or/on demand save the index to location specified

b) I assume that 

# Tests
Run the MAVEN tests after you start the test environment (prerequisites)