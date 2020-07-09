#!/usr/bin/env bash
set -eux

# Sanity test of the docker images.

# The root directory of the project.
PROJECT_DIRECTORY=$1
# The relative paths to the dockerfiles.
DOCKER_RESOURCE_HOMES=($2)
# Names of the Docker images to test.
IMAGE_NAMES=($3)
# The version of the image.
IMAGE_VERSION=$4
# The directories (within /opt) on the images that our products should have been installed in.
DIST_DIRS=($5)

# The number of projects being processed.
COUNT=${#IMAGE_NAMES[@]}

for (( i = 0; i < $COUNT; i++ ))
do
    IMAGE_NAME=${IMAGE_NAMES[$i]}
    DOCKER_RESOURCE_HOME=${DOCKER_RESOURCE_HOMES[$i]}
    # There is a variable called DIST_DIR in the image, so use something different.
    DIR=${DIST_DIRS[$i]}

    DOCKER_IMAGE_VERSIONED="$IMAGE_NAME:$IMAGE_VERSION"

    cd "$PROJECT_DIRECTORY/$DOCKER_RESOURCE_HOME"

    if [ "$DIR" != "zeppelin" ]
    then
        docker run --rm $DOCKER_IMAGE_VERSIONED [ -d /opt/$DIR/solr ] || (echo "solr dir does not exist" && exit 1)
        docker run --rm $DOCKER_IMAGE_VERSIONED [ -d /opt/$DIR/data/alfrescoModels ] || (echo "alfrescoModels dir does not exist" && exit 1)
        docker run --rm $DOCKER_IMAGE_VERSIONED [ -e /opt/$DIR/solr.in.sh ] || (echo "solr.in.sh does not exist" && exit 1)
        docker run --rm $DOCKER_IMAGE_VERSIONED grep -q Alfresco /opt/$DIR/solr.in.sh || (echo "solr.in.sh does not contain Alfresco config" && exit 1)
        docker run --rm $DOCKER_IMAGE_VERSIONED grep -q Alfresco /opt/$DIR/solr.in.cmd || (echo "solr.in.cmd does not contain Alfresco config" && exit 1)
        docker run --rm $DOCKER_IMAGE_VERSIONED grep -q LOG4J_PROPS /opt/$DIR/solr.in.sh || (echo "solr.in.sh does not contain LOG4J_PROPS" && exit 1)
        docker run --rm $DOCKER_IMAGE_VERSIONED grep -q LOG4J_CONFIG /opt/$DIR/solr.in.cmd || (echo "solr.in.cmd does not contain LOG4J_CONFIG" && exit 1)
        docker run --rm $DOCKER_IMAGE_VERSIONED [ -e /opt/$DIR/solrhome/conf/shared.properties ] || (echo "shared.properties does not exist" && exit 1)
        docker run --rm $DOCKER_IMAGE_VERSIONED /opt/$DIR/solr/bin/solr start
    fi
done
