ARG ZEPPELIN_TAG
FROM quay.io/alfresco/insight-zeppelin:${ZEPPELIN_TAG}

# COMMS
ARG ALFRESCO_COMMS
ENV ALFRESCO_COMMS $ALFRESCO_COMMS

# Useless when running in Alfresco Comms none mode
RUN mkdir ${ZEPPELIN_HOME}/keystore \
 && chown -R zeppelin:zeppelin ${ZEPPELIN_HOME}/keystore

### Add SSL Configuration to Zeppelin Interpreter
RUN if [ "$ALFRESCO_COMMS" == "https" ] ; then \
       sed -i '/"zeppelin.jdbc.principal":/i \
         "alfresco.enable.ssl": { \n\
           "value": "true", \n\
           "type": "string" \n\
         },\n\
         "solr.ssl.checkPeerName": {\n\
           "value": "false",\n\
           "type": "string"\n\
         },\n\
         "javax.net.ssl.keyStore": {\n\
           "value": "/zeppelin/keystore/ssl.repo.client.keystore",\n\
           "type": "string"\n\
         },\n\
         "javax.net.ssl.keyStorePassword": {\n\
           "value": "kT9X6oe68t",\n\
           "type": "string"\n\
         },\n\
         "javax.net.ssl.keyStoreType": {\n\
           "value": "JCEKS",\n\
           "type": "string"\n\
         },\n\
         "javax.net.ssl.trustStore": {\n\
           "value": "/zeppelin/keystore/ssl.repo.client.truststore",\n\
           "type": "string"\n\
         },\n\
         "javax.net.ssl.trustStorePassword": {\n\
           "value": "kT9X6oe68t",\n\
           "type": "string"\n\
         },\n\
         "javax.net.ssl.trustStoreType": {\n\
           "value": "JCEKS",\n\
           "type": "string"\n\
         \n},\
 ' ${ZEPPELIN_HOME}/conf/interpreter.json; \
fi
