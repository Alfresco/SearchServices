#!/bin/bash
set -e
# By default its going to deploy "Master" setup configuration with "REPLICATION_TYPE=master".
# Slave replica service can be enabled using "REPLICATION_TYPE=slave" environment value.

SOLR_CONFIG_FILE=$PWD/solrhome/templates/rerank/conf/solrconfig.xml

if [[ $REPLICATION_TYPE == "master" ]]; then

   findStringMaster='<requestHandler name="\/replication" class="org\.alfresco\.solr\.handler\.AlfrescoReplicationHandler">'

   replaceStringMaster="\n\t<lst name=\"master\"> \n"

   if [[ $REPLICATION_AFTER == "" ]]; then
      REPLICATION_AFTER=commit
   fi

   for i in $(echo $REPLICATION_AFTER | sed "s/,/ /g")
   do
      replaceStringMaster+="\t\t<str name=\"replicateAfter\">"$i"<\/str> \n"
   done

   if [[ ! -z "$REPLICATION_CONFIG_FILES" ]]; then
      replaceStringMaster+="\t\t<str name=\"confFiles\">$REPLICATION_CONFIG_FILES<\/str> \n"
   fi

   replaceStringMaster+="\t<\/lst>"

   sed -i "s/$findStringMaster/$findStringMaster$replaceStringMaster/g" $SOLR_CONFIG_FILE
fi

if [[ $REPLICATION_TYPE == "slave" ]]; then

   if [[ $REPLICATION_MASTER_PROTOCOL == "" ]]; then
      REPLICATION_MASTER_PROTOCOL=http
   fi

   if [[ $REPLICATION_MASTER_HOST == "" ]]; then
      REPLICATION_MASTER_HOST=localhost
   fi

   if [[ $REPLICATION_MASTER_PORT == "" ]]; then
      REPLICATION_MASTER_PORT=8083
   fi

   if [[ $REPLICATION_CORE_NAME == "" ]]; then
      REPLICATION_CORE_NAME=alfresco
   fi

   if [[ $REPLICATION_POLL_INTERVAL == "" ]]; then
      REPLICATION_POLL_INTERVAL=00:00:30
   fi

   sed -i 's/<requestHandler name="\/replication" class="org\.alfresco\.solr\.handler\.AlfrescoReplicationHandler">/<requestHandler name="\/replication" class="org\.alfresco\.solr\.handler\.AlfrescoReplicationHandler">\
      <lst name="slave">\
         <str name="masterUrl">'$REPLICATION_MASTER_PROTOCOL':\/\/'$REPLICATION_MASTER_HOST':'$REPLICATION_MASTER_PORT'\/solr\/'$REPLICATION_CORE_NAME'<\/str>\
         <str name="pollInterval">'$REPLICATION_POLL_INTERVAL'<\/str>\
      <\/lst>/g' $SOLR_CONFIG_FILE
fi

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

if [[ true == "$ENABLE_SPELLCHECK" ]]; then
   sed -i 's/#alfresco.suggestable.property/alfresco.suggestable.property/' ${PWD}/solrhome/conf/shared.properties
fi

bash -c "$@"