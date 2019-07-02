#!/usr/bin/env bash

REQ_HOST="localhost:9080"
REQ_TESTS_API="http://$REQ_HOST/alfresco-bm-manager/api/v1/tests"

echo -ne "Create Search Test\n"
#Create Search Test
TEST_NAME="SearchTest"
curl -s $REQ_TESTS_API -H "'Host: '"$REQ_HOST"/'" -H 'Content-Type: application/json;charset=utf-8' --data '{"name":"'$TEST_NAME'","description":"Users","release":"alfresco-bm-rest-api-3.0.1-SNAPSHOT","schema":"12"}'

echo -ne "Set MongoDB Search\n"
#Set Mongo DB
IP=$(hostname -I | awk '{print $1}')
curl -s $REQ_TESTS_API"/"$TEST_NAME"/props/mongo.test.host" -X PUT -H "'Host: '"$REQ_HOST"/'" -H 'Content-Type: application/json;charset=utf-8' --data '{"version":0,"value":"'$IP':27017"}'

echo "Set Alfresco related attributes Search"
#Set Alfresco related attributes
#ALF_SERVER=${bamboo.alfresco.url}
#ALF_SERVER="ec2-34-245-148-19.eu-west-1.compute.amazonaws.com"
ALF_SERVER=$(echo $ALFRESCO_URL | cut -d '/' -f3 | cut -d ':' -f1)
ALF_PORT=$(echo $ALFRESCO_URL | cut -d ':' -f3)
curl -s $REQ_TESTS_API"/"$TEST_NAME"/props/alfresco.server" -X PUT -H "'Host: '"$REQ_HOST"/'" -H 'Content-Type: application/json;charset=utf-8' --data '{"version":0,"value":"'$ALF_SERVER'"}'
curl -s $REQ_TESTS_API"/"$TEST_NAME"/props/alfresco.url" -X PUT -H "'Host: '"$REQ_HOST"/'" -H 'Content-Type: application/json;charset=utf-8' --data '{"version":0,"value":"http://'$ALF_SERVER:$ALF_PORT'/"}'
curl -s $REQ_TESTS_API"/"$TEST_NAME"/props/alfresco.port" -X PUT -H "'Host: '"$REQ_HOST"/'" -H 'Content-Type: application/json;charset=utf-8' --data '{"version":0,"value":"'$ALF_PORT'"}'

#Set Scenario Weightings
echo -ne "Set Scenario Weightings\n"
#curl -s $REQ_TESTS_API"/"$TEST_NAME"/props/weight.4.read" -X PUT -H "'Host: '"$REQ_HOST"/'" -H 'Content-Type: application/json;charset=utf-8' --data '{"version":0,"value":0}'
#curl -s $REQ_TESTS_API"/"$TEST_NAME"/props/weight.4.read" -X PUT -H "'Host: '"$REQ_HOST"/'" -H 'Content-Type: application/json;charset=utf-8' --data '{"version":0,"value":0}'
curl -s $REQ_TESTS_API"/"$TEST_NAME"/props/weight.read" -X PUT -H "'Host: '"$REQ_HOST"/'" -H 'Content-Type: application/json;charset=utf-8' --data '{"version":0,"value":0}'
curl -s $REQ_TESTS_API"/"$TEST_NAME"/props/weight.write" -X PUT -H "'Host: '"$REQ_HOST"/'" -H 'Content-Type: application/json;charset=utf-8' --data '{"version":0,"value":0}'


echo -ne "Create test instance Search\n"
#Create test instance
INSTANCE_NAME="RunTestSearch1"
curl -s $REQ_TESTS_API"/"$TEST_NAME"/runs" -H "'Host: '"$REQ_HOST"/'" -H 'Content-Type: application/json;charset=utf-8' --data '{"name":"'$INSTANCE_NAME'"}'

echo -ne "Start the test Search\n"
#Start the test
curl -s $REQ_TESTS_API"/"$TEST_NAME"/runs/"$INSTANCE_NAME"/schedule" -H "'Host: '"$REQ_HOST"/'" -H 'Content-Type: application/json;charset=utf-8' --data '{"version":0,"scheduled":0}'

completed=-1
# Add 0 to the completed value to ensure it's a number.
while [ $((completed+0)) -eq -1 ]
do
    sleep 10
    status=`curl -s $REQ_TESTS_API"/"$TEST_NAME"/runs" -H "'Host: '"$REQ_HOST"/'" -H 'Content-Type: application/json;charset=utf-8'`
    #log "Status: $status"
    # Update the completed timestamp value (which will be -1 if the test hasn't finished yet).
    completed=`echo $status | sed 's|^.*"completed" : \([^ ]*\) .*$|\1|g'`
    if [ $((completed+0)) -eq -1 ]
    then
        # Also check the "stopped" value if it hasn't completed.
        completed=`echo $status | sed 's|^.*"stopped" : \([^ ]*\) .*$|\1|g'`
    fi
    #log "Progress: "`echo $status | sed 's|^.*"progress" : \([^ ]*\) .*$|\1|g'`
done

#log "Output the CSV results."
curl -s -o search_results.xlsx -O -J $REQ_TESTS_API"/"$TEST_NAME"/runs/"$INSTANCE_NAME"/results/xlsx"
