# Alfresco Solr 6 Docker image
#
FROM java:8
MAINTAINER Gethin James, gethin.james@alfresco.com

ARG SOLR_ZIP
ENV DIST_DIR /opt

RUN groupadd -r solr && useradd -r -g solr -s /bin/bash solr

RUN apt-get update && apt-get install -y \
		ca-certificates \
		curl unzip lsof \
	--no-install-recommends && rm -r /var/lib/apt/lists/*

RUN mkdir -p $DIST_DIR/alfresco-solr-data
COPY $SOLR_ZIP $DIST_DIR/
WORKDIR $DIST_DIR

RUN unzip *.zip -d $DIST_DIR/ && rm *.zip && chown -R solr:solr $DIST_DIR/*
EXPOSE 8983
USER solr
CMD [ "/opt/alfresco-solr/bin/solr", "start", "-f" ]
