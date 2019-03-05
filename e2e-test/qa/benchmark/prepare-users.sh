#!/usr/bin/env bash
set -ex
REQ_HOST="localhost:9080"
REQ_TESTS_API="http://$REQ_HOST/alfresco-bm-manager/api/v1/tests"

TEST_NAME="SearchTest"
curl -s ${REQ_TESTS_API} -H 'Host: ${REQ_HOST}' -H 'Content-Type: application/json;charset=utf-8' --data '{"name":"'$TEST_NAME'","description":"Users","release":"alfresco-bm-rest-api-3.0.1-SNAPSHOT","schema":"12"}'

# add here content of https://git.alfresco.com/search_discovery/BuildScripts/blob/feature/search-1506/qa/benchmark/create-users.sh
# use ALFRESCO_URL for full http+alfresco+port =>http://localhost:8081 (see Makefile)
