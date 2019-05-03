#!/bin/bash
set -e

DIST_DIR=/opt/alfresco-insight-engine
SOLR_CONF_FOLDER=$DIST_DIR/solrhome/templates/rerank/conf
SOLR_CONFIG_FILE=$SOLR_CONF_FOLDER/solrconfig.xml
SOLR_CORE_FILE=$SOLR_CONF_FOLDER/solrcore.properties
REPLICATION_MASTER_HOST=search
REPLICATION_MASTER_PORT=8983

if [[ $REPLICATION_TYPE == "master" ]]; then

   findStringMaster='<requestHandler name="\/replication" class="solr\.ReplicationHandler" >'

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

   sed -i -e "s/$findStringMaster/$findStringMaster$replaceStringMaster/g" $SOLR_CONFIG_FILE
   sed -i -e "s/enable.alfresco.tracking=true/enable.alfresco.tracking=true\nenable.master=true\nenable.slave=false/g" $SOLR_CORE_FILE
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

   if [[ $REPLICATION_POLL_INTERVAL == "" ]]; then
      REPLICATION_POLL_INTERVAL=00:00:60
   fi

   sed -i -e 's/<requestHandler name="\/replication" class="solr\.ReplicationHandler" >/<requestHandler name="\/replication" class="solr\.ReplicationHandler" >\
      <lst name="slave">\
         <str name="masterUrl">'$REPLICATION_MASTER_PROTOCOL':\/\/'$REPLICATION_MASTER_HOST':'$REPLICATION_MASTER_PORT'\/solr\/${solr.core.name}<\/str>\
         <str name="pollInterval">'$REPLICATION_POLL_INTERVAL'<\/str>\
      <\/lst>/g' $SOLR_CONFIG_FILE
   sed -i -e "s/enable.alfresco.tracking=true/enable.alfresco.tracking=true\nenable.master=false\nenable.slave=true/g" $SOLR_CORE_FILE
fi

exec "$@"
