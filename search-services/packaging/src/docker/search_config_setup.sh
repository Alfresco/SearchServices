#!/bin/bash
set -e

SOLR_IN_FILE=$PWD/solr.in.sh

if [[ ! -z "$SOLR_HEAP" ]]; then
   sed -i -e "s/.*SOLR_HEAP=.*/SOLR_HEAP=\"$SOLR_HEAP\"/g" $SOLR_IN_FILE
fi

if [[ ! -z "$SOLR_JAVA_MEM" ]]; then
   sed -i -e "s/.*SOLR_JAVA_MEM=.*/SOLR_JAVA_MEM=\"$SOLR_JAVA_MEM\"/g" $SOLR_IN_FILE
fi

if [[ ! -z "$SOLR_SSL_ENABLED" ]]; then
   sed -i -e "s/.*SOLR_SSL_ENABLED=.*/SOLR_SSL_ENABLED=true/g" $SOLR_IN_FILE
   sed -i -e "s/.*SOLR_SSL_KEY_STORE=.*/SOLR_SSL_KEY_STORE=\/opt\/alfresco-search-services\/solrhome\/keystore\/ssl.repo.client.keystore/g" $SOLR_IN_FILE
   sed -i -e "s/.*SOLR_SSL_KEY_STORE_PASSWORD=.*/SOLR_SSL_KEY_STORE_PASSWORD=kT9X6oe68t/g" $SOLR_IN_FILE
   sed -i -e "s/.*SOLR_SSL_TRUST_STORE=.*/SOLR_SSL_TRUST_STORE=\/opt\/alfresco-search-services\/solrhome\/keystore\/ssl.repo.client.truststore/g" $SOLR_IN_FILE
   sed -i -e "s/.*SOLR_SSL_TRUST_STORE_PASSWORD=.*/SOLR_SSL_TRUST_STORE_PASSWORD=kT9X6oe68t/g" $SOLR_IN_FILE
   sed -i -e "s/.*SOLR_SSL_NEED_CLIENT_AUTH=.*/SOLR_SSL_NEED_CLIENT_AUTH=true/g" $SOLR_IN_FILE
   sed -i -e "s/.*SOLR_SSL_WANT_CLIENT_AUTH=.*/SOLR_SSL_WANT_CLIENT_AUTH=false/g" $SOLR_IN_FILE
fi

bash -c "$@"