ARG SEARCH_TAG
FROM <%=searchImage%>:${SEARCH_TAG}

ARG SOLR_HOSTNAME
ARG ENABLE_MASTER
ARG ENABLE_SLAVE
ARG MASTER_HOST
ENV SOLR_HOSTNAME $SOLR_HOSTNAME
ENV ENABLE_MASTER $ENABLE_MASTER
ENV ENABLE_SLAVE $ENABLE_SLAVE
ENV MASTER_HOST $MASTER_HOST

# Configure SOLR cores to run in HTTP mode from template
RUN sed -i '/^bash.*/i sed -i "'"s/alfresco.secureComms=https/alfresco.secureComms=none/g"'" ${DIST_DIR}/solrhome/templates/rerank/conf/solrcore.properties\n' \
    ${DIST_DIR}/solr/bin/search_config_setup.sh

# Configure Alfresco Service Name
RUN sed -i '/^bash.*/i sed -i "'"s/alfresco.host=localhost/alfresco.host=alfresco/g"'" ${DIST_DIR}/solrhome/templates/rerank/conf/solrcore.properties\n' \
    ${DIST_DIR}/solr/bin/search_config_setup.sh

# Set Hostname for this Node
RUN sed -i '/^bash.*/i sed -i "'"s/solr.host=localhost/solr.host=${SOLR_HOSTNAME}/g"'" ${DIST_DIR}/solrhome/conf/shared.properties\n' \
    ${DIST_DIR}/solr/bin/search_config_setup.sh

# Set Master / Slave configuration for this Node
RUN sed -i '/^bash.*/i echo "\nenable.master=${ENABLE_MASTER}\nenable.slave=${ENABLE_SLAVE}" >> ${DIST_DIR}/solrhome/templates/rerank/conf/solrcore.properties\n' \
${DIST_DIR}/solr/bin/search_config_setup.sh

RUN if [ "$ENABLE_MASTER" = "true" ] ; then \
sed -i "/^bash.*/i sed -i '/^\\\\\s*<requestHandler name=\"\\\\/replication\".*/a \
    <lst name=\"master\">\
      <str name=\"replicateAfter\">commit</str>\
      <str name=\"replicateAfter\">startup</str>\
      <str name=\"confFiles\">schema.xml,stopwords.txt</str>\
    </lst>' ${DIST_DIR}/solrhome/templates/rerank/conf/solrconfig.xml\n" ${DIST_DIR}/solr/bin/search_config_setup.sh; \
else \
sed -i "/^bash.*/i sed -i '/^\\\\\s*<requestHandler name=\"\\\\/replication\".*/a \
    <lst name=\"slave\">\
      <str name=\"masterUrl\">http://${MASTER_HOST}:8983/solr/alfresco</str>\
      <str name=\"pollInterval\">00:00:60</str>\
    </lst>' ${DIST_DIR}/solrhome/templates/rerank/conf/solrconfig.xml\n" ${DIST_DIR}/solr/bin/search_config_setup.sh; \
fi
