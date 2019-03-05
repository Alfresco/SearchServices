#!/usr/bin/env bash
set -ex
REQ_HOST="localhost:9080"
REQ_TESTS_API="http://$REQ_HOST/alfresco-bm-manager/api/v1/tests"

TEST_NAME="DataTest"

# add here content of https://git.alfresco.com/search_discovery/BuildScripts/blob/feature/search-1506/qa/benchmark/create-data.sh
# use ALFRESCO_URL for full http+alfresco+port =>http://localhost:8081 (see Makefile)

