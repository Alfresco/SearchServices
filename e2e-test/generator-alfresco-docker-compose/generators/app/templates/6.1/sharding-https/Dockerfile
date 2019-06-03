ARG SEARCH_TAG
FROM <%=searchImage%>:${SEARCH_TAG}

ARG SOLR_HOSTNAME
ARG NUM_SHARDS
ARG SHARD_ID
ARG PORT_NUMBER

ENV SOLR_HOSTNAME $SOLR_HOSTNAME
ENV NUM_SHARDS $NUM_SHARDS
ENV SHARD_ID $SHARD_ID
ENV PORT_NUMBER $PORT_NUMBER

# Configure SOLR cores to run in HTTP mode from template
RUN sed -i '/^bash.*/i sed -i "'"s/alfresco.secureComms=none/alfresco.secureComms=https/g"'" ${DIST_DIR}/solrhome/templates/rerank/conf/solrcore.properties\n' \
    ${DIST_DIR}/solr/bin/search_config_setup.sh

# Configure Alfresco Service Name
RUN sed -i '/^bash.*/i sed -i "'"s/alfresco.host=localhost/alfresco.host=alfresco/g"'" ${DIST_DIR}/solrhome/templates/rerank/conf/solrcore.properties\n' \
    ${DIST_DIR}/solr/bin/search_config_setup.sh

# Set Hostname for this Shard Service
RUN sed -i '/^bash.*/i sed -i "'"s/solr.host=localhost/solr.host=${SOLR_HOSTNAME}/g"'" ${DIST_DIR}/solrhome/conf/shared.properties\n' \
    ${DIST_DIR}/solr/bin/search_config_setup.sh

# Set Port Number and Sharding ID for this Shard Service
RUN sed -i '/^bash.*/i echo "\nsolr.port.ssl=${PORT_NUMBER}\nshard.count=${NUM_SHARDS}\nshard.instance=${SHARD_ID}" >> ${DIST_DIR}/solrhome/templates/rerank/conf/solrcore.properties\n' \
${DIST_DIR}/solr/bin/search_config_setup.sh

# Set Port Number and Sharding ID for this Shard Service
RUN sed -i '/^bash.*/i echo "\nalfresco.port=8080\nalfresco.port.ssl=8443\nalfresco.baseUrl=/alfresco" >> ${DIST_DIR}/solrhome/templates/rerank/conf/solrcore.properties\n' \
${DIST_DIR}/solr/bin/search_config_setup.sh


# Adding external keystore
RUN mkdir ${DIST_DIR}/keystore \
 && chown -R solr:solr ${DIST_DIR}/keystore

VOLUME ["${DIST_DIR}/keystore"]
