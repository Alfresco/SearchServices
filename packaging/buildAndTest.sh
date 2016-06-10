#!/bin/bash
set -eo pipefail

[ "$DEBUG" ] && set -x

# set current working directory to the directory of the script
cd "$(dirname "$0")"

dockerImage="dockerreg.alfresco.com/alfresco-solr:${bamboo_planRepository_1_branch}-latest"

rm -rf target/alfresco-solr
rm -rf target/solr-*
rm -rf target/dependency-maven-plugin-markers

echo "Building $dockerImage..."

docker build -t $dockerImage .

echo "Testing $dockerImage..."

if ! docker inspect "$dockerImage" &> /dev/null; then
    echo $'\timage does not exist!'
    false
fi

# running tests
docker run --rm "$dockerImage" [ -d /opt/alfresco-solr/data ] || (echo "Data dir does not exist" && exit 1)
docker run --rm "$dockerImage" [ -e /opt/alfresco-solr/solr/server/solr/conf/shared.properties ] || (echo "shared.properties does not exist" && exit 1)
docker run --rm "$dockerImage" /opt/alfresco-solr/solr/bin/solr start

echo "Publishing $dockerImage..."
docker push "$dockerImage"
echo "SUCCESS"
