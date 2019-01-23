#!/usr/bin/env bash
set -ex

echo "Enabling SpellCheck"
cat <<EOF >> /opt/alfresco-search-services/solrhome/conf/shared.properties

# Enabling SpellCheck
# configuration: 
# * http://docs.alfresco.com/6.0/concepts/solr-shared-properties.html
# * https://docs.alfresco.com/5.2/tasks/solr6-install-withoutSSL.html
# test it: http://docs.alfresco.com/6.0/concepts/search-api-spellcheck.html

# Suggestable Properties
alfresco.suggestable.property.0={http://www.alfresco.org/model/content/1.0}name
alfresco.suggestable.property.1={http://www.alfresco.org/model/content/1.0}title 
alfresco.suggestable.property.2={http://www.alfresco.org/model/content/1.0}description 
alfresco.suggestable.property.3={http://www.alfresco.org/model/content/1.0}content

EOF
