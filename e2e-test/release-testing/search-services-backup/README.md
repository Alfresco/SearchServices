# backup

Backup folder inside the Docker Container is named:
```
/opt/alfresco-search-services/backup
```

This folder is mapped as volume in local path:
```
./data/backup
```

Alfresco Admin Web Console settings for *Search Service > Backup Settings*:

* Backup Location: `/opt/alfresco-search-services/backup`
* Backup Cron Expression: `0 0/2 * * * ?`

URL to perform a backup:
http://127.0.0.1:8083/solr/alfresco/replication?command=backup&location=/opt/alfresco-search-services/backup/&numberToKeep=2&wt=xml

If using Sharding or Replication, additionally:
http://127.0.0.1:8084/solr/alfresco/replication?command=backup&location=/opt/alfresco-search-services/backup/&numberToKeep=2&wt=xml
