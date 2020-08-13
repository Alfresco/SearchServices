ARG ALFRESCO_TAG
FROM alfresco/alfresco-content-repository:${ALFRESCO_TAG}

ARG TOMCAT_DIR=/usr/local/tomcat
ARG ALF_DATA_DIR=${TOMCAT_DIR}/alf_data

# COMMS
ARG SOLR_COMMS
ENV SOLR_COMMS $SOLR_COMMS

# SSL
ARG TRUSTSTORE_TYPE
ARG TRUSTSTORE_PASS
ARG KEYSTORE_TYPE
ARG KEYSTORE_PASS

ENV TRUSTSTORE_TYPE=$TRUSTSTORE_TYPE \
    TRUSTSTORE_PASS=$TRUSTSTORE_PASS \
    KEYSTORE_TYPE=$KEYSTORE_TYPE \
    KEYSTORE_PASS=$KEYSTORE_PASS

# Expose keystore folder
# Useless for 'none'/'http' communications with SOLR
VOLUME ["${ALF_DATA_DIR}/keystore"]

USER root

# Install modules and addons
RUN mkdir -p $TOMCAT_DIR/amps
COPY modules/amps/* $TOMCAT_DIR/amps/
COPY modules/jars/* $TOMCAT_DIR/webapps/alfresco/WEB-INF/lib/
RUN java -jar $TOMCAT_DIR/alfresco-mmt/alfresco-mmt*.jar install \
    $TOMCAT_DIR/amps $TOMCAT_DIR/webapps/alfresco -directory -nobackup -force;

# Default value in "repository.properties" is "dir.keystore=classpath:alfresco/keystore"
RUN if [ "$SOLR_COMMS" == "https" ] ; then \
        echo -e "\n\
        dir.keystore=${ALF_DATA_DIR}/keystore\n\
        alfresco.encryption.ssl.keystore.type=${TRUSTSTORE_TYPE}\n\
        alfresco.encryption.ssl.truststore.type=${KEYSTORE_TYPE}\n\
        " >> ${TOMCAT_DIR}/shared/classes/alfresco-global.properties; \
    fi

# Enable SSL by adding the proper Connector to server.xml
RUN if [ "$SOLR_COMMS" == "https" ] ; then \
      sed -i "s/\
[[:space:]]\+<\/Engine>/\n\
        <\/Engine>\n\
        <Connector port=\"8443\" protocol=\"org.apache.coyote.http11.Http11Protocol\"\n\
            connectionTimeout=\"20000\"\n\        
            SSLEnabled=\"true\" maxThreads=\"150\" scheme=\"https\"\n\
            keystoreFile=\"\/usr\/local\/tomcat\/alf_data\/keystore\/ssl.keystore\"\n\
            keystorePass=\"${KEYSTORE_PASS}\" keystoreType=\"${KEYSTORE_TYPE}\" secure=\"true\"\n\
            truststoreFile=\"\/usr\/local\/tomcat\/alf_data\/keystore\/ssl.truststore\"\n\
            truststorePass=\"${TRUSTSTORE_PASS}\" truststoreType=\"${TRUSTSTORE_TYPE}\" clientAuth=\"want\" sslProtocol=\"TLS\">\n\
        <\/Connector>/g" ${TOMCAT_DIR}/conf/server.xml; \
    fi

# GZIP COMPRESSION
ARG COMPRESS_CONTENT
ENV COMPRESS_CONTENT $COMPRESS_CONTENT
RUN if [ "$COMPRESS_CONTENT" == "true" ] ; then \
      sed -i "s/\
[[:space:]]\+connectionTimeout=\"20000\"/\n\
        connectionTimeout=\"20000\"\n\
        compression=\"on\"\n\
        compressionMinSize=\"1\"\n\
        /g" ${TOMCAT_DIR}/conf/server.xml; \
    fi


# Copy custom content model to deployment folder
COPY model/* $TOMCAT_DIR/shared/classes/alfresco/extension/
