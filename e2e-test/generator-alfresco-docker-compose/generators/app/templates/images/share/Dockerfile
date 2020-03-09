ARG SHARE_TAG
FROM <%=shareImage%>:${SHARE_TAG}

ARG TOMCAT_DIR=/usr/local/tomcat

<% if (httpWebMode == 'https') { %>
RUN sed -i '/Connector port="8080"/a scheme="https" secure="true"' /usr/local/tomcat/conf/server.xml && \
    sed -i '/Connector port="8080"/a proxyName="localhost" proxyPort="<%=port%>"' /usr/local/tomcat/conf/server.xml
<% } %>

# Install modules
RUN mkdir -p $TOMCAT_DIR/amps
COPY modules/amps/* $TOMCAT_DIR/amps/
COPY modules/jars/* $TOMCAT_DIR/webapps/share/WEB-INF/lib/
RUN java -jar $TOMCAT_DIR/alfresco-mmt/alfresco-mmt*.jar install \
    $TOMCAT_DIR/amps $TOMCAT_DIR/webapps/share -directory -nobackup -force;

# Copy custom content forms to deployment folder
COPY model/* $TOMCAT_DIR/shared/classes/alfresco/web-extension/