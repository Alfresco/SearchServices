# Alfresco Solr 6 Docker image with banana
#
# To build your own banana image:
# export bamboo_planRepository_1_branch=local
# Change line 15 in buildandTest.sh to point to your local banana (hint: line starts: cp -rf /home/gethin/)
# ./buildandTest.sh
#
FROM docker-internal.alfresco.com/search-services:develop-latest
MAINTAINER Gethin James, gethin.james@alfresco.com

COPY banana-jetty-context.xml $DIST_DIR/solr/server/contexts/
COPY banana-dist $DIST_DIR/solr/server/solr-webapp/banana