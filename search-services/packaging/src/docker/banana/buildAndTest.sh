#!/bin/bash
set -e 

[ "$DEBUG" ] && set -x

# set current working directory to the directory of the script
cd "$(dirname "$0")"

nicebranch=`echo "$bamboo_planRepository_1_branch" | sed 's/\//_/'`
dockerImage="docker-internal.alfresco.com/search-services-banana:${nicebranch}-latest"

echo "Building $dockerImage..."

rm -rf ./banana-dist
cp -rf /home/gethin/covodev/source/banana/dist ./banana-dist
docker build -t $dockerImage .

# running tests
docker run --rm "$dockerImage" [ -d /opt/alfresco-solr/solr/server/solr-webapp/banana ] || (echo "banana dir does not exist" && exit 1)
docker run --rm "$dockerImage" [ -e /opt/alfresco-solr//solr/server/contexts/banana-jetty-context.xml ] || (echo "banana context does not exist" && exit 1)

if [ "${nicebranch}" == "local" ]
then
    echo "Skipping docker publish for local build"
else
    echo "Publishing $dockerImage..."
    docker push "$dockerImage"
fi

echo "Docker banana SUCCESS"
