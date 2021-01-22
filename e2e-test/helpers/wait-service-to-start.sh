#!/usr/bin/env bash
set -e  # exit if commands fails
set -x  # trace what gets exe

WAIT_INTERVAL=1
COUNTER=0
TIMEOUT=2000
t0=`date +%s`

declare -a endpoints=("${1:-http://localhost:8081/alfresco/}" "${1:-http://localhost:8083/solr/}")

for endpoint in "${endpoints[@]}"
do

   echo "Waiting for Service to start using endpoint: ${endpoint}"

   until [[ "$(curl --output /dev/null -w ''%{http_code}'' --silent --head --fail ${endpoint})" == 200 ]] || [ "$COUNTER" -eq "$TIMEOUT" ]; do
      printf '.'
      sleep $WAIT_INTERVAL
      COUNTER=$(($COUNTER+$WAIT_INTERVAL))
   done

   if (("$COUNTER" < "$TIMEOUT")) ; then
      t1=`date +%s`
      delta=$((($t1 - $t0)/60))
      echo "Service ${endpoint} Started in $delta minutes"
   else
      echo "Service ${endpoint} could not start in time."
      echo "Waited $COUNTER seconds"
      exit 1
   fi
done