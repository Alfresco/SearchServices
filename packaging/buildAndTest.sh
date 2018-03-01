#!/bin/bash
set -e

[ "$DEBUG" ] && set -x

# set current working directory to the directory of the script
cd "$(dirname "$0")"

nicebranch=`echo "$bamboo_planRepository_1_branch" | sed 's/\//_/'`
dockerImage="docker-internal.alfresco.com/search-services:$bamboo_maven_version"
echo "Building $dockerImage from $nicebranch using version $bamboo_maven_version"

docker build --build-arg solrBranch=$nicebranch --build-arg solrVer=$bamboo_maven_version -t $dockerImage src/docker

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

if [ "${nicebranch}" == "local" ]
then
    echo "Skipping docker publish for local build"
else
    echo "Publishing $dockerImage..."
    docker push "$dockerImage"
fi

echo "Docker SUCCESS"
