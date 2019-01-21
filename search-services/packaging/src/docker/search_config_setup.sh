#!/bin/bash
set -e

SOLR_IN_FILE=$PWD/solr.in.sh

if [[ ! -z "$SOLR_HEAP" ]]; then
   sed -i -e "s/.*SOLR_HEAP=.*/SOLR_HEAP=\"$SOLR_HEAP\"/g" $SOLR_IN_FILE
fi

if [[ ! -z "$SOLR_JAVA_MEM" ]]; then
   sed -i -e "s/.*SOLR_JAVA_MEM=.*/SOLR_JAVA_MEM=\"$SOLR_JAVA_MEM\"/g" $SOLR_IN_FILE
fi

if [ "$TLS_ENABLED" = "true" ] ; then
  echo "Setting up TLS"

  store_pass=${STORE_PASS}
  dist_dir="/opt/alfresco-search-services"
  keystore_dir=${KEYSTORE_DIR}

  mkdir $dist_dir/solrhome/keystore/
  cp $keystore_dir/* $dist_dir/solrhome/keystore/
  cp $keystore_dir/* $dist_dir/solrhome/templates/rerank/conf/
  cp $keystore_dir/* $dist_dir/solrhome/templates/aps/processDefinition/conf/
  cp $keystore_dir/* $dist_dir/solrhome/templates/aps/taskDefinition/conf/
  cp $keystore_dir/* $dist_dir/solrhome/templates/aps/process/conf/
  cp $keystore_dir/* $dist_dir/solrhome/templates/aps/task/conf/
  cp $keystore_dir/* $dist_dir/solrhome/templates/noRerank/conf/
  cp $keystore_dir/* $dist_dir/solrhome/templates/rerankWithQueryLog/rerank/conf/
  cp $keystore_dir/* $dist_dir/solrhome/templates/rerankWithQueryLog/qlog/conf/

  sed -i -e "s/.*SOLR_SSL_ENABLED=.*/SOLR_SSL_ENABLED=true/g" $SOLR_IN_FILE
  sed -i -e "s/.*SOLR_SSL_KEY_STORE=.*/SOLR_SSL_KEY_STORE=\/opt\/alfresco-search-services\/solrhome\/keystore\/ssl.repo.client.keystore/g" $SOLR_IN_FILE
  sed -i -e "s/.*SOLR_SSL_KEY_STORE_PASSWORD=.*/SOLR_SSL_KEY_STORE_PASSWORD=$store_pass/g" $SOLR_IN_FILE
  sed -i -e "s/.*SOLR_SSL_TRUST_STORE=.*/SOLR_SSL_TRUST_STORE=\/opt\/alfresco-search-services\/solrhome\/keystore\/ssl.repo.client.truststore/g" $SOLR_IN_FILE
  sed -i -e "s/.*SOLR_SSL_TRUST_STORE_PASSWORD=.*/SOLR_SSL_TRUST_STORE_PASSWORD=$store_pass/g" $SOLR_IN_FILE
  sed -i -e "s/.*SOLR_SSL_NEED_CLIENT_AUTH=.*/SOLR_SSL_NEED_CLIENT_AUTH=true/g" $SOLR_IN_FILE
  sed -i -e "s/.*SOLR_SSL_WANT_CLIENT_AUTH=.*/SOLR_SSL_WANT_CLIENT_AUTH=false/g" $SOLR_IN_FILE
fi

echo "Starting Solr search services"

bash -c "$@"