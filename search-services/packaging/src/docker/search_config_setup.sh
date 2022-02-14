#!/bin/bash
set -e
# By default its going to deploy "Master" setup configuration with "REPLICATION_TYPE=master".
# Slave replica service can be enabled using "REPLICATION_TYPE=slave" environment value.

log_warn() {
	echo -e " ====WARN==== \n$*\nWARN CODE was $LOG_WARN" >&2
}

RERANK_TEMPLATE_PATH=$PWD/solrhome/templates/rerank/conf
NORERANK_TEMPLATE_PATH=$PWD/solrhome/templates/noRerank/conf
SOLR_RERANK_CONFIG_FILE=$RERANK_TEMPLATE_PATH/solrconfig.xml
SOLR_NORERANK_CONFIG_FILE=$NORERANK_TEMPLATE_PATH/solrconfig.xml
SOLR_RERANK_CORE_FILE=$RERANK_TEMPLATE_PATH/solrcore.properties
SOLR_NORERANK_CORE_FILE=$NORERANK_TEMPLATE_PATH/solrcore.properties
SOLR_CONTEXT_FILE=$PWD/solr/server/contexts/solr-jetty-context.xml
LOG_PROPERTIES=$PWD/logs/log4j.properties

if [[ $REPLICATION_TYPE == "master" ]]; then

   findStringMaster='<requestHandler name="\/replication" class="org\.alfresco\.solr\.handler\.AlfrescoReplicationHandler">'

   replaceStringMaster="\n\t<lst name=\"master\"> \n"

   if [[ $REPLICATION_AFTER == "" ]]; then
      REPLICATION_AFTER=commit,startup
   fi

   if [[ $REPLICATION_CONFIG_FILES == "" ]]; then
      REPLICATION_CONFIG_FILES=schema.xml,stopwords.txt
   fi

   for i in $(echo $REPLICATION_AFTER | sed "s/,/ /g")
   do
      replaceStringMaster+="\t\t<str name=\"replicateAfter\">"$i"<\/str> \n"
   done

   if [[ ! -z "$REPLICATION_CONFIG_FILES" ]]; then
      replaceStringMaster+="\t\t<str name=\"confFiles\">$REPLICATION_CONFIG_FILES<\/str> \n"
   fi

   replaceStringMaster+="\t<\/lst>"

   sed -i "s/$findStringMaster/$findStringMaster$replaceStringMaster/g" $SOLR_RERANK_CONFIG_FILE $SOLR_NORERANK_CONFIG_FILE
   sed -i "s/enable.alfresco.tracking=true/enable.alfresco.tracking=true\nenable.master=true\nenable.slave=false/g" $SOLR_RERANK_CORE_FILE $SOLR_NORERANK_CORE_FILE
fi

if [[ $REPLICATION_TYPE == "slave" ]]; then

   if [[ $REPLICATION_MASTER_PROTOCOL != https ]]; then
      REPLICATION_MASTER_PROTOCOL=http
   fi

   if [[ $REPLICATION_MASTER_HOST == "" ]]; then
      REPLICATION_MASTER_HOST=localhost
   fi

   if [[ $REPLICATION_MASTER_PORT == "" ]]; then
      REPLICATION_MASTER_PORT=8083
   fi

   if [[ $REPLICATION_POLL_INTERVAL == "" ]]; then
      REPLICATION_POLL_INTERVAL=00:00:30
   fi

   sed -i 's/<requestHandler name="\/replication" class="org\.alfresco\.solr\.handler\.AlfrescoReplicationHandler">/<requestHandler name="\/replication" class="org\.alfresco\.solr\.handler\.AlfrescoReplicationHandler">\
      <lst name="slave">\
         <str name="masterUrl">'$REPLICATION_MASTER_PROTOCOL':\/\/'$REPLICATION_MASTER_HOST':'$REPLICATION_MASTER_PORT'\/solr\/${solr.core.name}<\/str>\
         <str name="pollInterval">'$REPLICATION_POLL_INTERVAL'<\/str>\
      <\/lst>/g' $SOLR_RERANK_CONFIG_FILE $SOLR_NORERANK_CONFIG_FILE
   sed -i "s/enable.alfresco.tracking=true/enable.alfresco.tracking=false\nenable.master=false\nenable.slave=true/g" $SOLR_RERANK_CORE_FILE $SOLR_NORERANK_CORE_FILE
   sed -i 's/default="\/solr"/default="\/solr-slave"/g' $SOLR_CONTEXT_FILE
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
# Plain HTTP with a secret word in the request header can be enabled by setting ALFRESCO_SECURE_COMMS to 'secret',
# the secret word should be defined as a JVM argument like so: JAVA_TOOL_OPTIONS="-Dalfresco.secureComms.secret=my-secret-value"
case "$ALFRESCO_SECURE_COMMS" in
   secret)
     sed -i "s/alfresco.secureComms=https/alfresco.secureComms=secret\n/" $SOLR_RERANK_CORE_FILE $SOLR_NORERANK_CORE_FILE
     if [[ -f ${PWD}/solrhome/alfresco/conf/solrcore.properties ]]; then
         sed -i "s/alfresco.secureComms=https/alfresco.secureComms=secret\n/" ${PWD}/solrhome/alfresco/conf/solrcore.properties
     fi
     if [[ -f ${PWD}/solrhome/archive/conf/solrcore.properties ]]; then
         sed -i "s/alfresco.secureComms=https/alfresco.secureComms=secret\n/" ${PWD}/solrhome/archive/conf/solrcore.properties
     fi
   ;;
   none)
     sed -i "s/alfresco.secureComms=https/alfresco.secureComms=none\n/" $SOLR_RERANK_CORE_FILE $SOLR_NORERANK_CORE_FILE
     if [[ -f ${PWD}/solrhome/alfresco/conf/solrcore.properties ]]; then
         sed -i "s/alfresco.secureComms=https/alfresco.secureComms=none\n/" ${PWD}/solrhome/alfresco/conf/solrcore.properties
     fi
     if [[ -f ${PWD}/solrhome/archive/conf/solrcore.properties ]]; then
         sed -i "s/alfresco.secureComms=https/alfresco.secureComms=none\n/" ${PWD}/solrhome/archive/conf/solrcore.properties
     fi
   ;;
   https|'')
   ;;
   *)
      LOG_WARN=1
   ;;
esac

[ -z $LOG_WARN ] || log_warn "something was wrong with the authentication config, defaulting to https mTLS auth.\nIf mTLS is not properly configured Search service might not work"

if [[ true == "$ENABLE_SPELLCHECK" ]]; then
   sed -i 's/#alfresco.suggestable.property/alfresco.suggestable.property/' ${PWD}/solrhome/conf/shared.properties
fi

if [[ true == "$DISABLE_CASCADE_TRACKING" ]]; then
   sed -i 's/alfresco.cascade.tracker.enabled=true/alfresco.cascade.tracker.enabled=false/' ${PWD}/solrhome/conf/shared.properties
fi

if [[ "${SEARCH_LOG_LEVEL}" != "" ]]; then
   sed -i "s/log4j.rootLogger=WARN, file, CONSOLE/log4j.rootLogger=${SEARCH_LOG_LEVEL}, file, CONSOLE/" ${LOG_PROPERTIES}
fi

bash -c "$@"
