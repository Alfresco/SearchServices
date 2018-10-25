# Alfresco Search Services ${project.version} Docker Image

FROM alfresco/alfresco-base-java:11-openjdk-centos-7
LABEL creator="Gethin James" maintainer="Alfresco Search Services Team"

ENV DIST_DIR /opt/alfresco-search-services
ENV SOLR_ZIP ${project.build.finalName}.zip
ENV LANG C.UTF-8
ENV OPENSSL_VERSION 1.0.2k-8.el7

COPY "$SOLR_ZIP" .

RUN groupadd solr && useradd -s /bin/bash -g solr solr
RUN set -x \
   && yum update -y \
   && yum install -y unzip \
   && yum install -y lsof ca-certificates openssl-$OPENSSL_VERSION \
   && yum clean all \
   && unzip "$SOLR_ZIP" -d /opt/ && rm "$SOLR_ZIP" \
   && mkdir -p $DIST_DIR/data \
   && mv $DIST_DIR/solrhome/alfrescoModels $DIST_DIR/data/ \
   && chown -R solr:solr $DIST_DIR \
   && echo '#Docker Setup' >> $DIST_DIR/solr.in.sh \
   && echo 'SOLR_OPTS="$SOLR_OPTS -Dsolr.data.dir.root=$DIST_DIR/data -Dsolr.solr.model.dir=$DIST_DIR/data/alfrescoModels"' >> $DIST_DIR/solr.in.sh

WORKDIR $DIST_DIR

VOLUME $DIST_DIR/data
VOLUME $DIST_DIR/solrhome

EXPOSE 8983
USER solr
CMD $DIST_DIR/solr/bin/solr start -f
