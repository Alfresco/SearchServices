FROM quay.io/alfresco/insight-zeppelin:1.1.0.1

RUN mkdir ${ZEPPELIN_HOME}/keystore \
 && chown -R zeppelin:zeppelin ${ZEPPELIN_HOME}/keystore

VOLUME ["${DIST_DIR}/keystore"]
