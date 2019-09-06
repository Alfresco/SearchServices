ARG SHARE_TAG
FROM <%=shareImage%>:${SHARE_TAG}

ARG TOMCAT_DIR=/usr/local/tomcat

# Copy custom content forms to deployment folder
COPY model/* $TOMCAT_DIR/shared/classes/alfresco/web-extension/