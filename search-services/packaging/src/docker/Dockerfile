# Alfresco Search Services ${project.version} Docker Image

FROM alfresco/alfresco-base-java:11.0.13-centos-7@sha256:c1e399d1bbb5d08e0905f1a9ef915ee7c5ea0c0ede11cc9bd7ca98532a9b27fa
LABEL creator="Alfresco" maintainer="Alfresco"

ENV DIST_DIR /opt/alfresco-search-services
ENV SOLR_ZIP ${project.build.finalName}.zip
ENV LANG C.UTF-8

# Get values from ENV VARS or use default values if ENV VARS are not specified
ENV SOLR_DATA_DIR_ROOT=${SOLR_DATA_DIR_ROOT:-$DIST_DIR/data}
ENV SOLR_SOLR_MODEL_DIR=${SOLR_SOLR_MODEL_DIR:-$DIST_DIR/data/alfrescoModels}

ARG USERNAME=solr
ARG USERID=33007

# YourKit Java Profiler
ARG JAVA_PROFILER=YourKit-JavaProfiler-2019.8-b142-docker

COPY "$SOLR_ZIP" .

RUN set -x \
   && useradd \
        -c "Alfresco ${USERNAME}" \
        -M \
        -s "/bin/bash" \
        -u "${USERID}" \
        -o \
        "${USERNAME}" \
   && yum install -y unzip \
   && yum install -y lsof ca-certificates \
   && yum install -y wget \
   && yum clean all \
   && unzip "$SOLR_ZIP" -d /opt/ && rm "$SOLR_ZIP" \
   && mkdir -p $DIST_DIR/data \
   && mv $DIST_DIR/solrhome/alfrescoModels $DIST_DIR/data/ \
   && chown -R ${USERNAME}:${USERNAME} $DIST_DIR \
   && echo '#Docker Setup' >> $DIST_DIR/solr.in.sh \
   && echo 'SOLR_OPTS="$SOLR_OPTS -Dsolr.data.dir.root=$SOLR_DATA_DIR_ROOT -Dsolr.solr.model.dir=$SOLR_SOLR_MODEL_DIR"' >> $DIST_DIR/solr.in.sh

COPY search_config_setup.sh $DIST_DIR/solr/bin/
RUN chmod +x $DIST_DIR/solr/bin/search_config_setup.sh

# Add the licenses to a root directory.
RUN mv $DIST_DIR/licenses /licenses

WORKDIR $DIST_DIR

VOLUME $DIST_DIR/data
VOLUME $DIST_DIR/solrhome
# Expose a folder to mount keystores in the host (required for Mutual TLS Auth)
VOLUME $DIST_DIR/keystores

# SOLR Service Port
EXPOSE 8983

# YourKit - Requires additional setting for the agent in JVM to be used
# For Docker Compose solr6 service, add the following lines:
#    SOLR_OPTS: "
#        -agentpath:/usr/local/YourKit-JavaProfiler-2019.8/bin/linux-x86-64/libyjpagent.so=port=10001,listen=all
#    "
#    ports:
#        - 10001:10001
RUN wget https://archive.yourkit.com/yjp/2019.8/${JAVA_PROFILER}.zip -P /tmp/ && \
  unzip /tmp/${JAVA_PROFILER}.zip -d /usr/local && \
  rm /tmp/${JAVA_PROFILER}.zip && \
  cp /usr/local/YourKit-JavaProfiler*/license-redist.txt /licenses/3rd-party/YourKit-license-redist.txt

# YourKit Profiling Port
EXPOSE 10001

USER ${USERNAME}
CMD $DIST_DIR/solr/bin/search_config_setup.sh "$DIST_DIR/solr/bin/solr start -f"
