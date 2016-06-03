#! /bin/bash
rm -rf target/alfresco-solr
rm -rf target/solr-*
export SOLR_VER=5.1-K_2-SNAPSHOT
docker build --build-arg SOLR_ZIP=target/alfresco-solr-${SOLR_VER}.zip -t dockerreg.alfresco.com/alfresco-solr:6.0${bamboo_buildNumber} .

echo "Now type: docker run --rm -p 8983:8983 dockerreg.alfresco.com/alfresco-solr:6.0${bamboo_buildNumber}"
