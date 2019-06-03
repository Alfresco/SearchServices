ARG SEARCH_TAG
FROM <%=searchImage%>:${SEARCH_TAG}

ARG SOLR_HOSTNAME
ARG NUM_SHARDS
ARG SHARD_ID
ENV SOLR_HOSTNAME $SOLR_HOSTNAME
ENV NUM_SHARDS $NUM_SHARDS
ENV SHARD_ID $SHARD_ID

# Configure SOLR cores to run in HTTP mode from template
RUN sed -i '/^bash.*/i sed -i "'"s/alfresco.secureComms=https/alfresco.secureComms=none/g"'" ${DIST_DIR}/solrhome/templates/rerank/conf/solrcore.properties\n' \
    ${DIST_DIR}/solr/bin/search_config_setup.sh

# Configure Alfresco Service Name
RUN sed -i '/^bash.*/i sed -i "'"s/alfresco.host=localhost/alfresco.host=alfresco/g"'" ${DIST_DIR}/solrhome/templates/rerank/conf/solrcore.properties\n' \
    ${DIST_DIR}/solr/bin/search_config_setup.sh

# Set Hostname for this Shard Service
RUN sed -i '/^bash.*/i sed -i "'"s/solr.host=localhost/solr.host=${SOLR_HOSTNAME}/g"'" ${DIST_DIR}/solrhome/conf/shared.properties\n' \
    ${DIST_DIR}/solr/bin/search_config_setup.sh

# Set Sharding ID for this Shard Service
RUN sed -i '/^bash.*/i echo "\nshard.count=${NUM_SHARDS}\nshard.instance=${SHARD_ID}" >> ${DIST_DIR}/solrhome/templates/rerank/conf/solrcore.properties\n' \
${DIST_DIR}/solr/bin/search_config_setup.sh
