# Alfresco Solr 6 Docker image
#
FROM java:8
MAINTAINER Gethin James, gethin.james@alfresco.com

ENV SOLR_ZIP target/alfresco-solr-distribution-*.zip
ENV DIST_DIR /opt/alfresco-solr
ENV SOLR_SOLR_CONTENT_DIR $DIST_DIR/data/content
ENV SOLR_SOLR_MODEL_DIR $DIST_DIR/data/alfrescoModels
ENV SOLR_INCLUDE $DIST_DIR/solr.in.sh

RUN groupadd -r solr && useradd -r -g solr -s /bin/bash solr

RUN apt-get update && apt-get install -y \
		ca-certificates \
		curl unzip lsof \
	--no-install-recommends && rm -r /var/lib/apt/lists/*

COPY $SOLR_ZIP $DIST_DIR/
COPY target/test-classes/solr.in.sh $DIST_DIR/
WORKDIR $DIST_DIR

RUN unzip *.zip -d $DIST_DIR/ && rm *.zip &&  \
        mkdir -p $SOLR_SOLR_CONTENT_DIR &&  \
        mkdir -p $DIST_DIR/data/index &&  \
        mv $DIST_DIR/solrhome/alfrescoModels $DIST_DIR/data/ &&  \
        mv $DIST_DIR/solrhome $DIST_DIR/data/ &&  \
        chown -R solr:solr $DIST_DIR

VOLUME /opt/alfresco-solr/data
EXPOSE 8983
USER solr
CMD [ "/opt/alfresco-solr/solr/bin/solr", "start", "-f" ]
