#!/bin/bash
set -e

SOLR_IN_FILE=$PWD/solr.in.sh

if [[ ! -z "$MAX_SOLR_RAM_PERCENTAGE" ]]; then
   MEM_CALC=$(expr $(cat /proc/meminfo | grep MemAvailable | awk '{print $2}') \* $MAX_SOLR_RAM_PERCENTAGE / 100)
   SOLR_MEM="-Xms${MEM_CALC}k -Xmx${MEM_CALC}k"
   sed -i -e "s/.*SOLR_JAVA_MEM=.*/SOLR_JAVA_MEM=\"${SOLR_MEM}\"/g" $SOLR_IN_FILE
fi

if [[ ! -z "$SOLR_HEAP" ]]; then
   sed -i -e "s/.*SOLR_HEAP=.*/SOLR_HEAP=\"$SOLR_HEAP\"/g" $SOLR_IN_FILE
fi


if [[ ! -z "$SOLR_JAVA_MEM" ]]; then
   sed -i -e "s/.*SOLR_JAVA_MEM=.*/SOLR_JAVA_MEM=\"$SOLR_JAVA_MEM\"/g" $SOLR_IN_FILE
fi

# By default Docker Image is using TLS Mutual Authentication (SSL) for communications with Repository
# Plain HTTP can be enabled by setting ALFRESCO_SECURE_COMMS to 'none'
if [[ "none" == "$ALFRESCO_SECURE_COMMS" ]]; then
   sed -i 's/alfresco.secureComms=https/alfresco.secureComms=none/' ${PWD}/solrhome/templates/rerank/conf/solrcore.properties
   sed -i 's/alfresco.secureComms=https/alfresco.secureComms=none/' ${PWD}/solrhome/templates/noRerank/conf/solrcore.properties
   # Apply also the setting to existing SOLR cores property files when existing
   if [[ -f ${PWD}/solrhome/alfresco/conf/solrcore.properties ]]; then
       sed -i 's/alfresco.secureComms=https/alfresco.secureComms=none/' ${PWD}/solrhome/alfresco/conf/solrcore.properties
   fi
   if [[ -f ${PWD}/solrhome/archive/conf/solrcore.properties ]]; then
       sed -i 's/alfresco.secureComms=https/alfresco.secureComms=none/' ${PWD}/solrhome/archive/conf/solrcore.properties
   fi
fi

bash -c "$@"