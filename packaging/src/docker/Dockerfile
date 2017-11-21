# Alfresco Solr Docker image
FROM quay.io/alfresco/alfresco-base-java:0.1.0
MAINTAINER Gethin James, gethin.james@alfresco.com

ENV DIST_DIR /opt/alfresco-search-services
ENV LANG C.UTF-8

COPY alfresco-search-services $DIST_DIR

RUN addgroup solr && adduser -s /bin/bash -D -G solr solr
RUN set -x \
	&& apk add --no-cache --virtual .fetch-deps \
		ca-certificates \
		vim unzip lsof \
		bash openssl wget \
	&& mkdir -p $DIST_DIR/data \
	&& mv $DIST_DIR/solrhome/alfrescoModels $DIST_DIR/data/  \
	&& chown -R solr:solr $DIST_DIR \
	&& echo '#Docker Setup' >> $DIST_DIR/solr.in.sh \
 	&& echo 'SOLR_OPTS="$SOLR_OPTS -Dsolr.data.dir.root=/opt/alfresco-search-services/data -Dsolr.solr.model.dir=/opt/alfresco-search-services/data/alfrescoModels"' >> $DIST_DIR/solr.in.sh

WORKDIR $DIST_DIR

VOLUME /opt/alfresco-search-services/data
VOLUME /opt/alfresco-search-services/solrhome

EXPOSE 8983
USER solr
CMD [ "/opt/alfresco-search-services/solr/bin/solr", "start", "-f" ]
