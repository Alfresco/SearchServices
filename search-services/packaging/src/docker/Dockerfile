# Alfresco Solr Docker image
FROM openjdk:8u111-jdk-alpine
MAINTAINER Gethin James, gethin.james@alfresco.com

ARG solrUrl
ARG solrBranch
ARG solrVer
ENV SOLR_URL ${solrUrl:-nightlybuilds.alfresco.com/SearchServices/$solrBranch/LATEST/Distribution/alfresco-search-services-$solrVer.zip}
ENV SOLR_ZIP alfresco-search-services.zip
ENV DIST_DIR /opt/alfresco-search
ENV LANG C.UTF-8

RUN addgroup solr && adduser -s /bin/bash -D -G solr solr
RUN set -x \
	&& apk add --no-cache --virtual .fetch-deps \
		ca-certificates \
		vim unzip lsof \
		bash openssl wget \
  && mkdir -p $DIST_DIR/data/content \
	&& wget --no-check-certificate -O "$SOLR_ZIP" "$SOLR_URL" \
	&& unzip "$SOLR_ZIP" -d $DIST_DIR/ && rm "$SOLR_ZIP"\
	&& mv $DIST_DIR/solrhome/alfrescoModels $DIST_DIR/data/  \
	&& chown -R solr:solr $DIST_DIR \
	&& echo '#Docker Setup' >> $DIST_DIR/solr.in.sh \
 	&& echo 'SOLR_OPTS="$SOLR_OPTS -Dsolr.data.dir.root=/opt/alfresco-search/data -Dsolr.solr.content.dir=/opt/alfresco-search/data/content -Dsolr.solr.model.dir=/opt/alfresco-search/data/alfrescoModels"' >> $DIST_DIR/solr.in.sh

WORKDIR $DIST_DIR

VOLUME /opt/alfresco-search/data
VOLUME /opt/alfresco-search/solrhome

EXPOSE 8983
USER solr
CMD [ "/opt/alfresco-search/solr/bin/solr", "start", "-f" ]
