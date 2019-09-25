package org.alfresco.solr.content;

import java.util.List;

public class ContentStoreCache {

    private static ContentStoreCache contentStoreCache = null;

    private List<ContentStoreTransaction> transactions;

    private String root;

    public void init(String root){
        this.root = root;
    }

    public String getContentStoreRootPath()  {
        return this.root;
    }

    public synchronized static ContentStoreCache get(){
        if (contentStoreCache == null)
        {
            contentStoreCache = new ContentStoreCache();
        }

        return contentStoreCache;
    }

    public class ContentStoreTransaction {
        private Long txId;
        private List<Long> deletion;
        private List<ContentStoreCacheEntry> updates;
    }

    public class ContentStoreCacheEntry {
        public long dbid;
        public long hash;
    }
}
