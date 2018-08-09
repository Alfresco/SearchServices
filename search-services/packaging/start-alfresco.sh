#!/usr/bin/env bash
# About:
#  Run docker-compose using appropriate docker-resources generated in target
#  The script is using the 'find' tool to search for a particular docker-compose.yml file
#  you can use also a filter-tag (partial strings from absolute path or docker-compose.yml) that will 
#  uniquely identify your desired docker-compose.yml file
#
# Usage: 
#  $ run.sh <docker-resource-folder> <clean-or-not> <filter-flag> <debug-or-not> <alfresco-endpoint>
#    * <docker-resource-folder>:  defaults to 'target'
#    * clean: will clean all running docker images on machine it will not start alfresco.
#    * <filter-flag>: can be 5.x or 6.x (defaults to 6.x) - it can be used to filter differed compose files
#    * <alfresco-endpoint>: the url of alfresco endpoint
#
# Examples:
#  $ run.sh - it will use latest docker-compose from this branch
#  $ run.sh target clean - it will clean the containers using the latest docker-compose from this branch
#  $ run.sh target clean 5.x - it will clean the containers using the 5.x. docker-compose file 
#  $ run.sh target up 5.x - will start alfresco using the 5.x docker-compose file
#  $ run.sh target up docker-resources/docker-compose.yml debug

echo `basename $0` called on `date` with arguments: "$@"

DOCKER_RESOURCES_PATH="${1:-target}"
CLEANUP="${2:-no-clean}"
FILTER_FLAG="${3:-6.x}" #5.x, 6.x or even docker-resources/docker-compose.yml (for release branches)
DOCKER_COMPOSE_FILE=$(find ${DOCKER_RESOURCES_PATH} -name "docker-compose.yml" -type f -exec realpath {} \;| grep ${FILTER_FLAG})
DEBUG="${4:-no-debug}"
ALFRESCO_ENDPOINT="${5:-http://localhost:8081/alfresco}"


# exit if docker-compose not found
[ ! -n "${DOCKER_COMPOSE_FILE}" ] && echo "docker-compose.yml file NOT FOUND in folder: '${DOCKER_RESOURCES_PATH}' using this filter flag: '${FILTER_FLAG}'"  && exit 1

DOCKER_RESOURCES_PATH=`dirname ${DOCKER_COMPOSE_FILE}` 

function wait_for_alfresco_to_start { 
    WAIT_INTERVAL=1
    COUNTER=0
    TIMEOUT=2000
    t0=`date +%s`

    echo "Waiting for Alfresco to start in docker container: ${ALFRESCO_ENDPOINT}"
    until $(curl --output /dev/null --silent --head --fail ${ALFRESCO_ENDPOINT}) || [ "$COUNTER" -eq "$TIMEOUT" ]; do
        printf '.'
        sleep $WAIT_INTERVAL
        COUNTER=$(($COUNTER+$WAIT_INTERVAL))
    done

    if (("$COUNTER" < "$TIMEOUT")) ; then
        t1=`date +%s`
        delta=$((($t1 - $t0)/60))
        echo "Alfresco Started in $delta minutes: ${ALFRESCO_ENDPOINT}"
    else
        echo "Waited $COUNTER seconds"
        echo "Alfresco Could not start in time."
        exit 1
    fi
}

function cleanup_containers {
    cd  ${DOCKER_RESOURCES_PATH} && docker-compose kill
    cd  ${DOCKER_RESOURCES_PATH} && docker-compose rm -fv    
}
function start_alfresco {    
    # update the basicAuthScheme https://issues.alfresco.com/jira/browse/REPO-2575
    sed -ie "s/-Dindex.subsystem.name=solr6/-Dindex.subsystem.name=solr6 -Dalfresco.restApi.basicAuthScheme=true/g" ${DOCKER_COMPOSE_FILE}

    # show the configuration of docker-compose.yml file that we will run
    cd  ${DOCKER_RESOURCES_PATH} && docker-compose config

    if [ ${DEBUG} = "debug" ]; then
        cd  ${DOCKER_RESOURCES_PATH} && docker-compose up
    else
        cd  ${DOCKER_RESOURCES_PATH} && docker-compose up -d
        wait_for_alfresco_to_start
    fi    
}

set -ex

if [ ${CLEANUP} = "clean" ]; then
    cleanup_containers
else    
    cleanup_containers
    start_alfresco
fi

