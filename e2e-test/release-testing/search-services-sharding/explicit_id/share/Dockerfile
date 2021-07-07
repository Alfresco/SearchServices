ARG SHARE_TAG
FROM quay.io/alfresco/alfresco-share:${SHARE_TAG}

ARG TOMCAT_DIR=/usr/local/tomcat



# Install modules
RUN mkdir -p $TOMCAT_DIR/amps
COPY modules/amps/* $TOMCAT_DIR/amps/
COPY modules/jars/* $TOMCAT_DIR/webapps/share/WEB-INF/lib/
RUN java -jar $TOMCAT_DIR/alfresco-mmt/alfresco-mmt*.jar install \
    $TOMCAT_DIR/amps $TOMCAT_DIR/webapps/share -directory -nobackup -force;

# Copy custom content forms to deployment folder
COPY model/* $TOMCAT_DIR/shared/classes/alfresco/web-extension/