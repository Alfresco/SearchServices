#!/usr/bin/env bash
set -ex
REQ_HOST="localhost:9080"
REQ_TESTS_API="http://$REQ_HOST/alfresco-bm-manager/api/v1/tests"
TEST_NAME="DataTest"

echo -ne "Create Test Data\n"
#Create Test
curl -s $REQ_TESTS_API -H "'Host: '"$REQ_HOST"/'" -H 'Content-Type: application/json;charset=utf-8' --data '{"name":"'$TEST_NAME'","description":"Data","release":"alfresco-bm-load-data-3.0.1-SNAPSHOT","schema":"11"}'

echo -ne "Set MongoDB Data\n"
#Set Mongo DB
IP=$(hostname -I | awk '{print $1}')
curl -s $REQ_TESTS_API"/"$TEST_NAME"/props/mongo.test.host" -X PUT -H "'Host: '"$REQ_HOST"/'" -H 'Content-Type: application/json;charset=utf-8' --data '{"version":0,"value":"'$IP':27017"}'

echo -ne "Set Alfresco related attributes Data\n"
#Set Alfresco related attributes
#ALF_SERVER=${bamboo.alfresco.url}
#ALF_SERVER="ec2-34-245-148-19.eu-west-1.compute.amazonaws.com"
ALF_SERVER=$(echo $ALFRESCO_URL | cut -d '/' -f3 | cut -d ':' -f1)
ALF_PORT=$(echo $ALFRESCO_URL | cut -d ':' -f3)
curl -s $REQ_TESTS_API"/"$TEST_NAME"/props/alfresco.server" -X PUT -H "'Host: '"$REQ_HOST"/'" -H 'Content-Type: application/json;charset=utf-8' --data '{"version":0,"value":"'$ALF_SERVER'"}'
curl -s $REQ_TESTS_API"/"$TEST_NAME"/props/alfresco.url" -X PUT -H "'Host: '"$REQ_HOST"/'" -H 'Content-Type: application/json;charset=utf-8' --data '{"version":0,"value":"'$ALFRESCO_URL'/"}'
curl -s $REQ_TESTS_API"/"$TEST_NAME"/props/alfresco.port" -X PUT -H "'Host: '"$REQ_HOST"/'" -H 'Content-Type: application/json;charset=utf-8' --data '{"version":0,"value":"'$ALF_PORT'"}'

#Set Site attribute
SITE_NAME=$(cat /dev/urandom | tr -dc 'a-zA-Z0-9' | fold -w 4 | head -n 1)-Site-%s-%05d
curl -s $REQ_TESTS_API"/"$TEST_NAME"/props/load.siteFormat" -X PUT -H "'Host: '"$REQ_HOST"/'" -H 'Content-Type: application/json;charset=utf-8' --data '{"version":0,"value":"'$SITE_NAME'"}'

echo -ne "Create test instance Data\n"
#Create test instance
INSTANCE_NAME="RunTestData1"
curl -s $REQ_TESTS_API"/"$TEST_NAME"/runs" -H "'Host: '"$REQ_HOST"/'" -H 'Content-Type: application/json;charset=utf-8' --data '{"name":"'$INSTANCE_NAME'"}'

echo -ne "Start the test Data\n"
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

curl -s -o create_data_results.xlsx -O -J $REQ_TESTS_API"/"$TEST_NAME"/runs/"$INSTANCE_NAME"/results/xlsx"