#!/bin/bash
set -e

[ "$DEBUG" ] && set -x

nicebranch=`echo "$bamboo_planRepository_1_branch" | sed 's/\//_/'`
DOCKER_RESOURCES_PATH="${1:-packaging/target/docker-resources}"
PUSH_IMAGE="${2:yes}"

if [ "${nicebranch}" = "master" ] || [ "${nicebranch#release}" != "${nicebranch}" ]
then   
   tag_version=`echo "$bamboo_maven_version"`
   if [ "${bamboo_shortJobName}" = "Release" ]
   then
      tag_version=`echo "$bamboo_release_version"`
   fi

   dockerImage="quay.io/alfresco/search-services:$tag_version"
   echo "Building $dockerImage from $nicebranch using version $tag_version"

   docker build -t $dockerImage ${DOCKER_RESOURCES_PATH}

   echo "Running tests"
   docker run --rm "$dockerImage" [ -d /opt/alfresco-search-services/solr ] || (echo "solr dir does not exist" && exit 1)
   docker run --rm "$dockerImage" [ -d /opt/alfresco-search-services/data/alfrescoModels ] || (echo "alfrescoModels dir does not exist" && exit 1)
   docker run --rm "$dockerImage" [ -e /opt/alfresco-search-services/solr.in.sh ] || (echo "solr.in.sh does not exist" && exit 1)
   docker run --rm "$dockerImage" grep -q Alfresco /opt/alfresco-search-services/solr.in.sh || (echo "solr.in.sh does not contain Alfresco config" && exit 1)
   docker run --rm "$dockerImage" grep -q Alfresco /opt/alfresco-search-services/solr.in.cmd || (echo "solr.in.cmd does not containAlfresco config" && exit 1)
   docker run --rm "$dockerImage" grep -q LOG4J_PROPS /opt/alfresco-search-services/solr.in.sh || (echo "solr.in.sh does not contain LOG4J_PROPS" && exit 1)
   docker run --rm "$dockerImage" grep -q LOG4J_CONFIG /opt/alfresco-search-services/solr.in.cmd || (echo "solr.in.cmd does not contain LOG4J_CONFIG" && exit 1)
   docker run --rm "$dockerImage" [ -e /opt/alfresco-search-services/solrhome/conf/shared.properties ] || (echo "shared.properties does not exist" && exit 1)
   docker run --rm "$dockerImage" /opt/alfresco-search-services/solr/bin/solr start

   if [ ${PUSH_IMAGE} = "yes" ]; then
        echo "Publishing $dockerImage..."
        docker push "$dockerImage"
   fi 
   
   echo "Docker SUCCESS"
else
    echo "Only building and publishing docker images from master or release branches. Skipping for '${nicebranch}'"
fi
