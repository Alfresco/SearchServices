# Alfresco Solr 6 Docker image
#
FROM openjdk:8
MAINTAINER Gethin James, gethin.james@alfresco.com

ENV SOLR_ZIP alfresco-solr-distribution-*.zip
ENV DIST_DIR /opt/alfresco-solr

RUN groupadd -r solr && useradd -m -r -g solr -s /bin/bash solr

RUN apt-get update && apt-get install -y \
		ca-certificates \
		vim curl unzip lsof \
	--no-install-recommends && rm -r /var/lib/apt/lists/*

COPY $SOLR_ZIP $DIST_DIR/
WORKDIR $DIST_DIR

RUN unzip *.zip -d $DIST_DIR/ && rm *.zip &&  \
        mkdir -p $DIST_DIR/data/content &&  \
        mv $DIST_DIR/solrhome/alfrescoModels $DIST_DIR/data/ &&  \
        chown -R solr:solr $DIST_DIR

RUN echo '\n##Docker Setup' >> $DIST_DIR/solr.in.sh \
 && echo 'SOLR_OPTS="$SOLR_OPTS -Dsolr.data.dir.root=/opt/alfresco-solr/data -Dsolr.solr.content.dir=/opt/alfresco-solr/data/content -Dsolr.solr.model.dir=/opt/alfresco-solr/data/alfrescoModels"' >> $DIST_DIR/solr.in.sh
#&& echo 'SOLR_HOME=$DIST_DIR/solrhome' >> $DIST_DIR/solr.in.sh \

VOLUME /opt/alfresco-solr/data
EXPOSE 8983
USER solr
CMD [ "/opt/alfresco-solr/solr/bin/solr", "start", "-f" ]
