# Alfresco Solr Docker image SSL
FROM docker-internal.alfresco.com/platform:latest
MAINTAINER Gethin James, gethin.james@alfresco.com

RUN cd /opt/alfresco/alf_data/keystore && chmod +x generate_keystores.sh \
    && ./generate_keystores.sh
RUN mkdir -p /opt/alfresco/sharedkeystore \
    && cp /opt/alfresco/alf_data/keystore/* /opt/alfresco/sharedkeystore/ \
    && cp /root/ssl.repo.client.keystore /opt/alfresco/sharedkeystore/ \
    && cp /root/ssl.repo.client.truststore /opt/alfresco/sharedkeystore/ \
    && cp /root/ssl.keystore /opt/alfresco/sharedkeystore/ \
    && cp /root/ssl.truststore /opt/alfresco/sharedkeystore/ \
    && chown -R alfresco:alfresco /opt/alfresco/sharedkeystore

VOLUME /opt/alfresco/sharedkeystore
