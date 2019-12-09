ARG SHARE_TAG
FROM <%=shareImage%>:${SHARE_TAG}

ARG TOMCAT_DIR=/usr/local/tomcat

<% if (httpWebMode == 'https') { %>
RUN sed -i '/Connector port="8080"/a scheme="https" secure="true"' /usr/local/tomcat/conf/server.xml && \
    sed -i '/Connector port="8080"/a proxyName="localhost" proxyPort="<%=port%>"' /usr/local/tomcat/conf/server.xml
<% } %>

# Copy custom content forms to deployment folder
COPY model/* $TOMCAT_DIR/shared/classes/alfresco/web-extension/