set -ex
# author: paul brodner
#
# it seems 'archive' and 'alfresco' cores are using the rerank templates (/opt/alfresco-search-services/solrhome/templates/rerank/) by default
# When solr starts and 'archive' and 'alfresco' cores are created
# data from /opt/alfresco-search-services/solrhome/templates/rerank/ is applied

echo "Enabling SSL"
cat <<EOF >> /opt/alfresco-search-services/solrhome/templates/rerank/conf/solrcore.properties

alfresco.port=7070
alfresco.protocol=https
alfresco.port.ssl=7070
alfresco.secureComms=https

solr.port=8443        
solr.port.ssl=8443
solr.secureComms=https
solr.solrConnectTimeout=5000
enable.alfresco.tracking=true

EOF
