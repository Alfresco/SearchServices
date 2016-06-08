#! /bin/bash
rm -rf target/alfresco-solr
rm -rf target/solr-*
rm -rf target/dependency-maven-plugin-markers

docker build -t dockerreg.alfresco.com/alfresco-solr:6.0${bamboo_buildNumber} -t dockerreg.alfresco.com/alfresco-solr:latest .

echo "Now type: docker run --rm -p 8983:8983 dockerreg.alfresco.com/alfresco-solr:6.0${bamboo_buildNumber}"
