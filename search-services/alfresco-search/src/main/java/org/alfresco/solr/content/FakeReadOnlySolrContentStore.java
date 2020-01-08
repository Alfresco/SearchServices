package org.alfresco.solr.content;

import org.alfresco.solr.client.NodeMetaData;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.search.LegacyNumericRangeQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.core.SolrCore;
import org.apache.solr.response.DocsStreamer;
import org.apache.solr.schema.IndexSchema;
import org.apache.solr.schema.SchemaField;
import org.apache.solr.search.SolrIndexSearcher;
import org.apache.solr.util.RefCounted;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.Optional.ofNullable;

/**
 * Note: this class extends SolrContentStore and not directly ContentStore in
 * order to minimise the impact on the existing code.
 */
public class FakeReadOnlySolrContentStore extends SolrContentStore
{
    private final static Logger LOGGER = LoggerFactory.getLogger(SolrContentStore.class);
    private final SolrCore core;

    // /tmp/alfresco is just a dummy path, it is not used at all by this content store.
    public FakeReadOnlySolrContentStore(SolrCore core)
    {
        super("/tmp");
        this.core = core;
    }

    @Override
    public SolrInputDocument retrieveDocFromSolrContentStore(String tenant, long dbId)
    {
        LOGGER.info("retrieveDocFromSolrContentStore, tenant {}, DBID {}", tenant, dbId);
        RefCounted<SolrIndexSearcher> ref = null;
        try
        {
            ref = core.getSearcher();
            SolrIndexSearcher searcher = ref.get();
            Query q = LegacyNumericRangeQuery.newLongRange("DBID", dbId, dbId + 1, true, false);
            TopDocs docs = searcher.search(q, 1);

            LOGGER.info("Q => " + q + " ( results => " + docs.totalHits + ")");
            if(docs.totalHits == 1)
            {
                ScoreDoc scoreDoc = docs.scoreDocs[0];
                SolrDocument document = convertLuceneDocToSolrDocument(searcher.doc(scoreDoc.doc), core.getLatestSchema());

                Set<String> dvFields =
                        core.getLatestSchema()
                            .getFields()
                            .entrySet()
                            .stream()
                            .map(Map.Entry::getKey)
                            .collect(Collectors.toSet());

                searcher.getDocFetcher().decorateDocValueFields(document, scoreDoc.doc, dvFields);

                return convertSolrDocToSolrInputDoc(document);
            }
        } catch (IOException e)
        {
            LOGGER.error("Failed to get doc from store using tenant {} and DBID {}", tenant, dbId, e);
            return null;
        } finally {
            ofNullable(ref).ifPresent(RefCounted::decref);
        }
        return null;
    }

    public static SolrDocument convertLuceneDocToSolrDocument(Document doc, final IndexSchema schema)
    {
        SolrDocument out = new SolrDocument();
        for (IndexableField f : doc.getFields())
        {

            SchemaField sf = schema.getFieldOrNull(f.name());
            Object value = DocsStreamer.getValue(sf, f);
            Object existing = out.get(f.name());

            if (existing == null) {
                if (sf != null && sf.multiValued())
                {
                    List<Object> vals = new ArrayList<>();
                    vals.add(value);
                    out.setField(f.name(), vals);
                } else
                {
                    out.setField(f.name(), value);
                }
            } else
            {
                out.addField(f.name(), value);
            }
        }
        return out;
    }

    private static SolrInputDocument convertSolrDocToSolrInputDoc(SolrDocument doc)
    {
        SolrInputDocument out = new SolrInputDocument();
        for (String f : doc.getFieldNames())
        {
            out.setField(f, doc.getFieldValue(f));
        }
        return out;
    }

    @Override
    public void storeDocOnSolrContentStore(String tenant, long dbId, SolrInputDocument doc)
    {
        LOGGER.info("(NoOp) StoreDocOnSolrContentStore (tenant = {}, dbId = {}, SolrInputDocument doc = {})", tenant, dbId, doc.getField("DBID"));
        // Do nothing, as this just a read-only view of the hosting Solr index
    }

    @Override
    public void storeDocOnSolrContentStore(NodeMetaData nodeMetaData, SolrInputDocument doc)
    {
        LOGGER.info("(NoOp) StoreDocOnSolrContentStore(nodeMetaData = {}, doc = {})", nodeMetaData, doc.getField("DBID"));
        // Do nothing, as this just a read-only view of the hosting Solr index
    }

    @Override
    public void removeDocFromContentStore(NodeMetaData nodeMetaData)
    {
        LOGGER.info("(NoOp) RemoveDocFromContentStore (metadata = {})", nodeMetaData);
        // Do nothing, as this just a read-only view of the hosting Solr index
    }

    @Override
    public String getRootLocation()
    {
        return null;
    }

    @Override
    public boolean exists(String s)
    {
        return false;
    }
}