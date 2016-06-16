#!/usr/bin/env bash
# Path to a directory for Solr to store cores and their data. By default, Solr will use server/solr
# This directory needs to contain solr.xml
SOLR_HOME=$DIST_DIR/data/solrhome
SOLR_OPTS="$SOLR_OPTS -Dsolr.data.dir.root=/opt/alfresco-solr/data/indexroot -Dsolr.solr.content.dir=/opt/alfresco-solr/data/content -Dsolr.solr.model.dir=/opt/alfresco-solr/data/alfrescoModels"