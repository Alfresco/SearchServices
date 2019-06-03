ARG SEARCH_TAG
FROM <%=searchImage%>:${SEARCH_TAG}

ARG TRUSTSTORE_TYPE
ENV TRUSTSTORE_TYPE $TRUSTSTORE_TYPE

ARG KEYSTORE_TYPE
ENV KEYSTORE_TYPE $KEYSTORE_TYPE

# Configure SOLR cores to run in HTTPs mode from template
RUN sed -i '/^bash.*/i sed -i "'"s/alfresco.secureComms=none/alfresco.secureComms=https/g"'" ${DIST_DIR}/solrhome/templates/rerank/conf/solrcore.properties\n' \
    ${DIST_DIR}/solr/bin/search_config_setup.sh

# Set SSL properties
RUN sed -i '/^bash.*/i \
    sed -i "'"s/alfresco.encryption.ssl.keystore.location=.*/alfresco.encryption.ssl.keystore.location=\\\/opt\\\/<%=searchPath%>\\\/keystore\\\/ssl.repo.client.keystore/g"'" ${DIST_DIR}/solrhome/templates/rerank/conf/solrcore.properties && \
    sed -i "'"s/alfresco.encryption.ssl.keystore.passwordFileLocation=.*/alfresco.encryption.ssl.keystore.passwordFileLocation=\\\/opt\\\/<%=searchPath%>\\\/keystore\\\/ssl-keystore-passwords.properties/g"'" ${DIST_DIR}/solrhome/templates/rerank/conf/solrcore.properties && \
    sed -i "'"s/alfresco.encryption.ssl.keystore.type=.*/alfresco.encryption.ssl.keystore.type=${KEYSTORE_TYPE}/g"'" ${DIST_DIR}/solrhome/templates/rerank/conf/solrcore.properties && \
    sed -i "'"s/alfresco.encryption.ssl.truststore.location=.*/alfresco.encryption.ssl.truststore.location=\\\/opt\\\/<%=searchPath%>\\\/keystore\\\/ssl.repo.client.truststore/g"'" ${DIST_DIR}/solrhome/templates/rerank/conf/solrcore.properties && \
    sed -i "'"s/alfresco.encryption.ssl.truststore.passwordFileLocation=.*/alfresco.encryption.ssl.truststore.passwordFileLocation=\\\/opt\\\/<%=searchPath%>\\\/keystore\\\/ssl-truststore-passwords.properties/g"'" ${DIST_DIR}/solrhome/templates/rerank/conf/solrcore.properties && \
    sed -i "'"s/alfresco.encryption.ssl.truststore.type=.*/alfresco.encryption.ssl.truststore.type=${TRUSTSTORE_TYPE}/g"'" ${DIST_DIR}/solrhome/templates/rerank/conf/solrcore.properties' \
    ${DIST_DIR}/solr/bin/search_config_setup.sh

# Remove pre-bundled stores (just to be sure that they are not picked up)
RUN sed -i '/^bash.*/i \
    rm ${DIST_DIR}/solrhome/templates/rerank/conf/ssl.repo.client.keystore && \
    rm ${DIST_DIR}/solrhome/templates/rerank/conf/ssl-keystore-passwords.properties && \
    rm ${DIST_DIR}/solrhome/templates/rerank/conf/ssl.repo.client.truststore && \
    rm ${DIST_DIR}/solrhome/templates/rerank/conf/ssl-truststore-passwords.properties' \
    ${DIST_DIR}/solr/bin/search_config_setup.sh

RUN mkdir ${DIST_DIR}/keystore \
 && chown -R solr:solr ${DIST_DIR}/keystore

VOLUME ["${DIST_DIR}/keystore"]
