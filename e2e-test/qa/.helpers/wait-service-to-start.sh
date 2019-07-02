#!/usr/bin/env bash
set -e  # exit if commands fails
set -x  # trace what gets exe

WAIT_INTERVAL=1
COUNTER=0
TIMEOUT=2000
t0=`date +%s`

# this is the default value, but you can override this passing the new endpoint as argument: $ wait-service-to-start.sh http://localhost:8083/solr
ENDPOINT="${1:-http://localhost:8081/alfresco}"

echo "Waiting for Service to start using endpoint: ${ENDPOINT}"

until $(curl --output /dev/null --silent --head --fail ${ENDPOINT}) || [ "$COUNTER" -eq "$TIMEOUT" ]; do
   printf '.'
   sleep $WAIT_INTERVAL
   COUNTER=$(($COUNTER+$WAIT_INTERVAL))
done

if (("$COUNTER" < "$TIMEOUT")) ; then
   t1=`date +%s`
   delta=$((($t1 - $t0)/60))
   echo "Service ${ENDPOINT} Started in $delta minutes"
else   
   echo "Service ${ENDPOINT} could not start in time."
   echo "Waited $COUNTER seconds"
   exit 1
fi
