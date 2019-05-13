ARG ALFRESCO_TAG
FROM alfresco/alfresco-content-repository-community:${ALFRESCO_TAG}

USER root

ARG TOMCAT_DIR=/usr/local/tomcat
ARG ALF_DATA_DIR=${TOMCAT_DIR}/alf_data

# Copy generated keystore to Image
COPY keystore ${ALF_DATA_DIR}/keystore

# Default value in "repository.properties" is "dir.keystore=classpath:alfresco/keystore"
RUN echo "dir.keystore=${ALF_DATA_DIR}/keystore" >> ${TOMCAT_DIR}/shared/classes/alfresco-global.properties

### Enable SSL by adding the proper Connector to server.xml
RUN sed -i "s/\
    <\/Engine>/\n\
    <\/Engine>\n\
    <Connector port=\"8443\" protocol=\"org.apache.coyote.http11.Http11Protocol\"\n\
          SSLEnabled=\"true\" maxThreads=\"150\" scheme=\"https\"\n\
          keystoreFile=\"\/usr\/local\/tomcat\/alf_data\/keystore\/ssl.keystore\"\n\
          keystorePass=\"kT9X6oe68t\" keystoreType=\"JCEKS\" secure=\"true\" connectionTimeout=\"240000\"\n\
          truststoreFile=\"\/usr\/local\/tomcat\/alf_data\/keystore\/ssl.truststore\"\n\
          truststorePass=\"kT9X6oe68t\" truststoreType=\"JCEKS\" clientAuth=\"want\" sslProtocol=\"TLS\">\n\
      <\/Connector>/g" ${TOMCAT_DIR}/conf/server.xml
