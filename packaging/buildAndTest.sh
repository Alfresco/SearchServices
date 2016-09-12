#!/bin/bash
set -e 

[ "$DEBUG" ] && set -x

# set current working directory to the directory of the script
cd "$(dirname "$0")"

nicebranch=`echo "$bamboo_planRepository_1_branch" | sed 's/\//_/'`
dockerImage="docker-internal.alfresco.com/alfresco-solr:${nicebranch}-latest"


rm -rf target/alfresco-solr
rm -rf target/solr-*
rm -rf target/dependency-maven-plugin-markers

echo "Building $dockerImage..."

docker build -t $dockerImage .

# running tests
docker run --rm "$dockerImage" [ -d /opt/alfresco-solr/solr ] || (echo "solr dir does not exist" && exit 1)
docker run --rm "$dockerImage" [ -d /opt/alfresco-solr/data/content ] || (echo "content dir does not exist" && exit 1)
docker run --rm "$dockerImage" [ -d /opt/alfresco-solr/data/alfrescoModels ] || (echo "alfrescoModels dir does not exist" && exit 1)
docker run --rm "$dockerImage" [ -e /opt/alfresco-solr/solr.in.sh ] || (echo "solr.in.sh does not exist" && exit 1)
docker run --rm "$dockerImage" [ -e /opt/alfresco-solr/solrhome/conf/shared.properties ] || (echo "shared.properties does not exist" && exit 1)
docker run --rm "$dockerImage" /opt/alfresco-solr/solr/bin/solr start

echo "Publishing $dockerImage..."
docker push "$dockerImage"
echo "Docker SUCCESS"
