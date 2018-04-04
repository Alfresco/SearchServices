#!/bin/bash
set -e

[ "$DEBUG" ] && set -x

nicebranch=`echo "$bamboo_planRepository_1_branch" | sed 's/\//_/'`

if [ "${nicebranch}" = "master" ] || [ "${nicebranch#release}" != "${nicebranch}" ]
then
   # set current working directory to the directory of the script
   cd "$bamboo_working_directory"

   docker_registry="quay.io/alfresco/search-services"
   tag_version=`echo "$bamboo_maven_version"`
   if [ "${bamboo_shortJobName}" = "Release" ]
   then
      tag_version=`echo "$bamboo_release_version"`
      docker_registry="alfresco/alfresco-search-services"
   fi

   dockerImage="$docker_registry:$tag_version"
   echo "Building $dockerImage from $nicebranch using version $tag_version"

   docker build -t $dockerImage packaging/target/docker-resources

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

   echo "Publishing $dockerImage..."
   docker push "$dockerImage"
   
   echo "Docker SUCCESS"
else
    echo "Only building and publishing docker images from master or release branches. Skipping for '${nicebranch}'"
fi
