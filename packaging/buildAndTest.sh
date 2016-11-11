#!/bin/bash
set -e 

[ "$DEBUG" ] && set -x

# set current working directory to the directory of the script
cd "$(dirname "$0")"

nicebranch=`echo "$bamboo_planRepository_1_branch" | sed 's/\//_/'`
dockerImage="docker-internal.alfresco.com/search-services:${nicebranch}-latest"

echo "Building $dockerImage..."

rm -f src/docker/alfresco-search-services-*.zip
cp target/alfresco-search-services-*.zip src/docker
docker build -t $dockerImage src/docker

# running tests
docker run --rm "$dockerImage" [ -d /opt/alfresco-solr/solr ] || (echo "solr dir does not exist" && exit 1)
docker run --rm "$dockerImage" [ -d /opt/alfresco-solr/data/content ] || (echo "content dir does not exist" && exit 1)
docker run --rm "$dockerImage" [ -d /opt/alfresco-solr/data/alfrescoModels ] || (echo "alfrescoModels dir does not exist" && exit 1)
docker run --rm "$dockerImage" [ -e /opt/alfresco-solr/solr.in.sh ] || (echo "solr.in.sh does not exist" && exit 1)
docker run --rm "$dockerImage" grep -q alfresco /opt/alfresco-solr/solr.in.sh || (echo "solr.in.sh does not contain alfresco config" && exit 1)
docker run --rm "$dockerImage" grep -q alfresco /opt/alfresco-solr/solr.in.cmd || (echo "solr.in.cmd does not contain alfresco config" && exit 1)
docker run --rm "$dockerImage" [ -e /opt/alfresco-solr/solrhome/conf/shared.properties ] || (echo "shared.properties does not exist" && exit 1)
docker run --rm "$dockerImage" /opt/alfresco-solr/solr/bin/solr start

if [ "${nicebranch}" == "local" ]
then
    echo "Skipping docker publish for local build"
else
    echo "Publishing $dockerImage..."
    docker push "$dockerImage"
fi

echo "Docker SUCCESS"
