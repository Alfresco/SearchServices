# sharding-replica

Once Docker Compose template is started, type following URLs in the browser:

http://127.0.0.1:8083/solr/admin/cores?action=newCore&storeRef=workspace://SpacesStore&numShards=3&numNodes=3&nodeInstance=1&template=rerank&property.data.dir.root=<>&shardIds=0,1&property.alfresco.port=8080
http://127.0.0.1:8084/solr/admin/cores?action=newCore&storeRef=workspace://SpacesStore&numShards=3&numNodes=3&nodeInstance=2&template=rerank&property.data.dir.root=<>&shardIds=1,2&property.alfresco.port=8080
http://127.0.0.1:8085/solr/admin/cores?action=newCore&storeRef=workspace://SpacesStore&numShards=3&numNodes=3&nodeInstance=3&template=rerank&property.data.dir.root=<>&shardIds=0,2&property.alfresco.port=8080

This will create 3 Shards with 2 core replicas on each one.
