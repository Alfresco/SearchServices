/*
 * #%L
 * Alfresco Search Services
 * %%
 * Copyright (C) 2005 - 2020 Alfresco Software Limited
 * %%
 * This file is part of the Alfresco software. 
 * If the software was purchased under a paid Alfresco license, the terms of 
 * the paid license agreement will prevail.  Otherwise, the software is 
 * provided under the following open source license terms:
 * 
 * Alfresco is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * Alfresco is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with Alfresco. If not, see <http://www.gnu.org/licenses/>.
 * #L%
 */

package org.alfresco.solr;

import static java.util.Arrays.asList;
import static java.util.Arrays.stream;
import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;
import static org.alfresco.repo.search.adaptor.QueryConstants.FIELD_ACLID;
import static org.alfresco.repo.search.adaptor.QueryConstants.FIELD_ACLTXCOMMITTIME;
import static org.alfresco.repo.search.adaptor.QueryConstants.FIELD_ACLTXID;
import static org.alfresco.repo.search.adaptor.QueryConstants.FIELD_ANAME;
import static org.alfresco.repo.search.adaptor.QueryConstants.FIELD_ANCESTOR;
import static org.alfresco.repo.search.adaptor.QueryConstants.FIELD_APATH;
import static org.alfresco.repo.search.adaptor.QueryConstants.FIELD_ASPECT;
import static org.alfresco.repo.search.adaptor.QueryConstants.FIELD_ASSOCTYPEQNAME;
import static org.alfresco.repo.search.adaptor.QueryConstants.FIELD_CASCADE_FLAG;
import static org.alfresco.repo.search.adaptor.QueryConstants.FIELD_DBID;
import static org.alfresco.repo.search.adaptor.QueryConstants.FIELD_DENIED;
import static org.alfresco.repo.search.adaptor.QueryConstants.FIELD_DOC_TYPE;
import static org.alfresco.repo.search.adaptor.QueryConstants.FIELD_EXCEPTION_MESSAGE;
import static org.alfresco.repo.search.adaptor.QueryConstants.FIELD_EXCEPTION_STACK;
import static org.alfresco.repo.search.adaptor.QueryConstants.FIELD_FIELDS;
import static org.alfresco.repo.search.adaptor.QueryConstants.FIELD_GEO;
import static org.alfresco.repo.search.adaptor.QueryConstants.FIELD_INACLTXID;
import static org.alfresco.repo.search.adaptor.QueryConstants.FIELD_INTXID;
import static org.alfresco.repo.search.adaptor.QueryConstants.FIELD_ISNODE;
import static org.alfresco.repo.search.adaptor.QueryConstants.FIELD_LID;
import static org.alfresco.repo.search.adaptor.QueryConstants.FIELD_NPATH;
import static org.alfresco.repo.search.adaptor.QueryConstants.FIELD_NULLPROPERTIES;
import static org.alfresco.repo.search.adaptor.QueryConstants.FIELD_OWNER;
import static org.alfresco.repo.search.adaptor.QueryConstants.FIELD_PARENT;
import static org.alfresco.repo.search.adaptor.QueryConstants.FIELD_PARENT_ASSOC_CRC;
import static org.alfresco.repo.search.adaptor.QueryConstants.FIELD_PATH;
import static org.alfresco.repo.search.adaptor.QueryConstants.FIELD_PNAME;
import static org.alfresco.repo.search.adaptor.QueryConstants.FIELD_PRIMARYASSOCQNAME;
import static org.alfresco.repo.search.adaptor.QueryConstants.FIELD_PRIMARYASSOCTYPEQNAME;
import static org.alfresco.repo.search.adaptor.QueryConstants.FIELD_PRIMARYPARENT;
import static org.alfresco.repo.search.adaptor.QueryConstants.FIELD_PROPERTIES;
import static org.alfresco.repo.search.adaptor.QueryConstants.FIELD_QNAME;
import static org.alfresco.repo.search.adaptor.QueryConstants.FIELD_READER;
import static org.alfresco.repo.search.adaptor.QueryConstants.FIELD_SITE;
import static org.alfresco.repo.search.adaptor.QueryConstants.FIELD_SOLR4_ID;
import static org.alfresco.repo.search.adaptor.QueryConstants.FIELD_S_ACLTXCOMMITTIME;
import static org.alfresco.repo.search.adaptor.QueryConstants.FIELD_S_ACLTXID;
import static org.alfresco.repo.search.adaptor.QueryConstants.FIELD_S_INACLTXID;
import static org.alfresco.repo.search.adaptor.QueryConstants.FIELD_S_INTXID;
import static org.alfresco.repo.search.adaptor.QueryConstants.FIELD_S_TXCOMMITTIME;
import static org.alfresco.repo.search.adaptor.QueryConstants.FIELD_S_TXID;
import static org.alfresco.repo.search.adaptor.QueryConstants.FIELD_TAG;
import static org.alfresco.repo.search.adaptor.QueryConstants.FIELD_TAG_SUGGEST;
import static org.alfresco.repo.search.adaptor.QueryConstants.FIELD_TENANT;
import static org.alfresco.repo.search.adaptor.QueryConstants.FIELD_TXCOMMITTIME;
import static org.alfresco.repo.search.adaptor.QueryConstants.FIELD_TXID;
import static org.alfresco.repo.search.adaptor.QueryConstants.FIELD_TYPE;
import static org.alfresco.repo.search.adaptor.QueryConstants.FIELD_VERSION;
import static org.alfresco.solr.AlfrescoSolrDataModel.getAclChangeSetDocumentId;
import static org.alfresco.solr.AlfrescoSolrDataModel.getAclDocumentId;
import static org.alfresco.solr.utils.Utils.notNullOrEmpty;
import static org.alfresco.util.ISO8601DateFormat.isTimeComponentDefined;
import static org.alfresco.service.cmr.security.AuthorityType.EVERYONE;
import static org.alfresco.service.cmr.security.AuthorityType.GROUP;
import static org.alfresco.service.cmr.security.AuthorityType.GUEST;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.IsoFields;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

import com.carrotsearch.hppc.IntArrayList;
import com.carrotsearch.hppc.LongHashSet;
import com.carrotsearch.hppc.cursors.LongCursor;

import org.alfresco.httpclient.AuthenticationException;
import org.alfresco.model.ContentModel;
import org.alfresco.opencmis.dictionary.CMISStrictDictionaryService;
import org.alfresco.repo.dictionary.DictionaryComponent;
import org.alfresco.repo.dictionary.M2Model;
import org.alfresco.repo.dictionary.NamespaceDAO;
import org.alfresco.repo.search.adaptor.QueryConstants;
import org.alfresco.service.cmr.dictionary.AspectDefinition;
import org.alfresco.service.cmr.dictionary.DataTypeDefinition;
import org.alfresco.service.cmr.dictionary.PropertyDefinition;
import org.alfresco.service.cmr.dictionary.TypeDefinition;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.datatype.DefaultTypeConverter;
import org.alfresco.service.cmr.security.AuthorityType;
import org.alfresco.service.namespace.QName;
import org.alfresco.solr.AlfrescoSolrDataModel.FieldInstance;
import org.alfresco.solr.AlfrescoSolrDataModel.IndexedField;
import org.alfresco.solr.AlfrescoSolrDataModel.TenantDbId;
import org.alfresco.solr.adapters.IOpenBitSet;
import org.alfresco.solr.adapters.ISimpleOrderedMap;
import org.alfresco.solr.adapters.SolrOpenBitSetAdapter;
import org.alfresco.solr.adapters.SolrSimpleOrderedMap;
import org.alfresco.solr.client.AclChangeSet;
import org.alfresco.solr.client.AclReaders;
import org.alfresco.solr.client.AlfrescoModel;
import org.alfresco.solr.client.ContentPropertyValue;
import org.alfresco.solr.client.MLTextPropertyValue;
import org.alfresco.solr.client.MultiPropertyValue;
import org.alfresco.solr.client.Node;
import org.alfresco.solr.client.Node.SolrApiNodeStatus;
import org.alfresco.solr.client.NodeMetaData;
import org.alfresco.solr.client.NodeMetaDataParameters;
import org.alfresco.solr.client.PropertyValue;
import org.alfresco.solr.client.SOLRAPIClient;
import org.alfresco.solr.client.SOLRAPIClient.GetTextContentResponse;
import org.alfresco.solr.client.StringPropertyValue;
import org.alfresco.solr.client.Transaction;
import org.alfresco.solr.config.ConfigUtil;
import org.alfresco.solr.logging.Log;
import org.alfresco.solr.tracker.IndexHealthReport;
import org.alfresco.solr.tracker.TrackerStats;
import org.alfresco.solr.utils.Utils;
import org.alfresco.util.ISO9075;
import org.alfresco.util.Pair;
import org.apache.commons.io.input.BoundedInputStream;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexCommit;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.NumericDocValues;
import org.apache.lucene.index.ReaderUtil;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.LeafCollector;
import org.apache.lucene.search.LegacyNumericRangeQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.TopFieldCollector;
import org.apache.lucene.util.BytesRefBuilder;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.SolrInputField;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.params.FacetParams;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.common.util.SimpleOrderedMap;
import org.apache.solr.core.IndexDeletionPolicyWrapper;
import org.apache.solr.core.SolrCore;
import org.apache.solr.core.SolrInfoMBean;
import org.apache.solr.request.LocalSolrQueryRequest;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.request.SolrRequestHandler;
import org.apache.solr.response.BasicResultContext;
import org.apache.solr.response.ResultContext;
import org.apache.solr.response.SolrQueryResponse;
import org.apache.solr.schema.FieldType;
import org.apache.solr.schema.SchemaField;
import org.apache.solr.search.DelegatingCollector;
import org.apache.solr.search.DocIterator;
import org.apache.solr.search.DocList;
import org.apache.solr.search.QueryWrapperFilter;
import org.apache.solr.search.SolrIndexSearcher;
import org.apache.solr.update.AddUpdateCommand;
import org.apache.solr.update.CommitUpdateCommand;
import org.apache.solr.update.DeleteUpdateCommand;
import org.apache.solr.update.RollbackUpdateCommand;
import org.apache.solr.update.processor.UpdateRequestProcessor;
import org.apache.solr.util.ConcurrentLRUCache;
import org.apache.solr.util.RefCounted;
import org.json.JSONException;
import org.springframework.extensions.surf.util.I18NUtil;
import org.springframework.util.FileCopyUtils;

/**
 * This is the Apache Solr implementation of the information server (index).
 *
 * @author Ahmed Owian
 * @since 5.0
 */
public class SolrInformationServer implements InformationServer
{
    /**
     * A specialization of {@link SolrInputDocument} which allows to represent a partial document (used for issuing
     * atomic update requests) and interact with it using the same interface of a full document.
     *
     * The semantic is a little bit specialised so this class it's not meant to be reused outside
     * the {@link SolrInformationServer}.
     *
     * Three methods have been overridden in order to fulfil the following behaviour:
     *
     * <ul>
     *     <li>
     *         {@link #removeField(String)}: removing a field on a full {@link SolrInputDocument} instance makes no
     *         much sense. However, the previous code used the local content store document "image" in order to rebuild
     *         the whole object: that's the reason why sometimes you will see this method called for "cleaning" up the
     *         previous existing values before setting a field.
     *         A call of {@link #removeField(String)} on a {@link PartialSolrInputDocument} instance will have the effect
     *         to put a "delete all" (i.e. "set" command with null value) marker value on the given field.
     *         That means if later some other method is called (e.g. {@link #setField(String, Object)},
     *         {@link #addField(String, Object)}) then the marker will be replaced, otherwise, when the document will
     *         be sent for indexing the field will be deleted.
     *
     *         IMPORTANT: a call to this method will set a "set" field modifier (with null value) on this field.
     *         That means any subsequent {@link #addField(String, Object)} call will add a value using the SAME field
     *         modifier. In other words at the end, when the document will be sent to indexing, the collected value(s)
     *         will replaced (set) the existing indexed value(s).
     *     </li>
     *     <li>
     *         {@link #addField(String, Object)}: this method call can have a double behaviour depending on if we
     *         previously called {@link #removeField(String)} on this instance or not.
     *         If we didn't call the {@link #removeField(String)} then the usual semantic applies: the collected  value(s)
     *         are added to a field modifier (i.e. "add" command) which indicates we want to merge them with the
     *         indexed value(s).
     *         Instead, as explained in the previous point above, if {@link #removeField(String)} has been called on this
     *         instance for a given field, then a "set" field modifier will be set and associated to that field, and
     *         subsequent call to {@link #addField(String, Object)} will collect the incoming values under the "set" command.
     *         In this latter case, when the document will be sent for indexing, the collected value(s) will replace the
     *         indexed one(s).
     *     </li>
     *     <li>
     *         {@link #setField(String, Object)}: the semantic doesn't change. A call to this method will indicate we want
     *         to replace the indexed field value(s). Note this method doesn't collect the input values, so that means
     *         unless you call it with a list, any subsequent call will replace the existing value.
     *     </li>
     * </ul>
     *
     * It has been introduced a new method "keepField". If this method is called, an explicit atomic update is executed
     * instead of a standard atomic updated.
     *
     * An explicit atomic update is an atomic update with the difference that we must specify all the fields that we want
     * to keep from the document already indexed in solr.
     *
     */
    static class PartialSolrInputDocument extends SolrInputDocument
    {
        private static final Map<String, String> KEEP_MAP = Map.of("keep", "");

        /**
         * Keep the field from the indexed solr document.
         *
         * Calling this method at least once provokes the execution of an explicit atomic update.
         * With explicit atomic update, all the fields defined in the indexed solr document
         * that are not explicitly inserted into the inputDocument will be discarded.
         */
        public void keepField(String name)
        {
            setField(name, KEEP_MAP);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void addField(String name, Object value)
        {
            Map<String, List<Object>> fieldModifier =
                    (Map<String, List<Object>>)computeIfAbsent(name, k -> {
                        remove(name);
                        setField(name, newFieldModifier("set"));

                        return getField(name);
                    }).getValue();

            ofNullable(value)
                    .ifPresent(v -> fieldModifier.computeIfAbsent(
                                                    fieldModifier.keySet().iterator().next(),
                                                    LAZY_EMPTY_MUTABLE_LIST).add(v));
        }

        @Override
        public SolrInputField removeField(String name)
        {
            setField(name, newFieldModifier("set"));
            return getField(name);
        }

        /**
         * Creates a new field modifier description for inserting an atomic update command within a document.
         *
         * @return the field modifier (a {@link Map} instance with a null value associated to the "set" command).
         */
        private Map<String, List<String>> newFieldModifier(String op)
        {
            return new HashMap<>()
            {{
                put(op, null);
            }};
        }
    }

    private static final Log LOGGER = new Log(SolrInformationServer.class);

    private static final String NO_SITE = "_REPOSITORY_";
    private static final String SHARED_FILES = "_SHARED_FILES_";

    private static final Set<String> REQUEST_ONLY_ID_FIELD = new HashSet<>(Collections.singletonList(FIELD_SOLR4_ID));

    private static final String LATEST_APPLIED_CONTENT_VERSION_ID = "LATEST_APPLIED_CONTENT_VERSION_ID";
    private static final String LAST_INCOMING_CONTENT_VERSION_ID = "LAST_INCOMING_CONTENT_VERSION_ID";

    private static final long CONTENT_OUTDATED_MARKER = -10;
    private static final long CONTENT_UPDATED_MARKER = -20;

    private static final String CONTENT_LOCALE_FIELD = "content@s__locale@{http://www.alfresco.org/model/content/1.0}content";
    private static final Set<String> ID_AND_CONTENT_VERSION_ID_AND_CONTENT_LOCALE =
            new HashSet<>(asList(FIELD_SOLR4_ID, LATEST_APPLIED_CONTENT_VERSION_ID, CONTENT_LOCALE_FIELD));

    private static final String INDEX_CAP_ID = "TRACKER!STATE!CAP";

    private static final Pattern CAPTURE_SITE = Pattern.compile("^/\\{http\\://www\\.alfresco\\.org/model/application/1\\.0\\}company\\_home/\\{http\\://www\\.alfresco\\.org/model/site/1\\.0\\}sites/\\{http\\://www\\.alfresco\\.org/model/content/1\\.0}([^/]*)/.*" );
    private static final Pattern CAPTURE_TAG = Pattern.compile("^/\\{http\\://www\\.alfresco\\.org/model/content/1\\.0\\}taggable/\\{http\\://www\\.alfresco\\.org/model/content/1\\.0\\}([^/]*)/\\{\\}member");
    private static final Pattern CAPTURE_SHARED_FILES = Pattern.compile("^/\\{http\\://www\\.alfresco\\.org/model/application/1\\.0\\}company\\_home/\\{http\\://www\\.alfresco\\.org/model/application/1\\.0\\}shared/.*" );

    public static final String AND = " AND ";
    public static final String OR = " OR ";

    static final String REQUEST_HANDLER_NATIVE = "/native";

    static final String REQUEST_HANDLER_GET = "/get";
    static final String RESPONSE_DEFAULT_ID = "doc";
    static final String RESPONSE_DEFAULT_IDS = "response";

    static final String PREFIX_ERROR = "ERROR-";

    public static final String DOC_TYPE_NODE = "Node";
    private static final String DOC_TYPE_UNINDEXED_NODE = "UnindexedNode";
    private static final String DOC_TYPE_ERROR_NODE = "ErrorNode";
    public static final String DOC_TYPE_ACL = "Acl";
    public static final String DOC_TYPE_TX = "Tx";
    public static final String DOC_TYPE_ACL_TX = "AclTx";
    private static final String DOC_TYPE_STATE = "State";

    public static final String SOLR_HOST = "solr.host";
    public static final String SOLR_PORT = "solr.port";
    public static final String SOLR_BASEURL = "solr.baseurl";

    /* 4096 is 2 to the power of (6*2), and we do this because the precision step for the long is 6,
     * and the transactions are long
     */
    private static final int BATCH_FACET_TXS = 4096;
    private static final String FINGERPRINT_FIELD = "MINHASH";
    /** Shared property to determine if the cascade tracking is enabled. */
    public static final String CASCADE_TRACKER_ENABLED = "alfresco.cascade.tracker.enabled";

    private static final String UNIT_OF_TIME_FIELD_INFIX = "_unit_of_time";
    public static final String UNIT_OF_TIME_YEAR_FIELD_SUFFIX = UNIT_OF_TIME_FIELD_INFIX + "_year";
    public static final String UNIT_OF_TIME_QUARTER_FIELD_SUFFIX = UNIT_OF_TIME_FIELD_INFIX + "_quarter";
    public static final String UNIT_OF_TIME_MONTH_FIELD_SUFFIX = UNIT_OF_TIME_FIELD_INFIX + "_month";
    public static final String UNIT_OF_TIME_DAY_FIELD_SUFFIX = UNIT_OF_TIME_FIELD_INFIX + "_day_of_month";
    public static final String UNIT_OF_TIME_DAY_OF_WEEK_FIELD_SUFFIX = UNIT_OF_TIME_FIELD_INFIX + "_day_of_week";
    public static final String UNIT_OF_TIME_DAY_OF_YEAR_FIELD_SUFFIX = UNIT_OF_TIME_FIELD_INFIX + "_day_of_year";
    public static final String UNIT_OF_TIME_HOUR_FIELD_SUFFIX = UNIT_OF_TIME_FIELD_INFIX + "_hour";
    public static final String UNIT_OF_TIME_MINUTE_FIELD_SUFFIX = UNIT_OF_TIME_FIELD_INFIX + "_minute";
    public static final String UNIT_OF_TIME_SECOND_FIELD_SUFFIX = UNIT_OF_TIME_FIELD_INFIX + "_second";

    private final static Function<String, List<Object>> LAZY_EMPTY_MUTABLE_LIST = key -> new ArrayList<>();

    private final AlfrescoCoreAdminHandler adminHandler;
    private final SolrCore core;
    private final SolrRequestHandler nativeRequestHandler;
    private final Cloud cloud;
    private final TrackerStats trackerStats = new TrackerStats(this);
    private final AlfrescoSolrDataModel dataModel;
    private final boolean contentIndexingHasBeenEnabledOnThisInstance;
    private final boolean recordUnindexedNodes;
    private final long lag;
    private final long holeRetention;
    private final boolean fingerprintHasBeenEnabledOnThisInstance;
    private final int contentStreamLimit;

    private long cleanContentLastPurged;
    
    // Get Paths information from Repository for a batch of nodes (true by default)
    // When false, Paths information is only recovered for single nodes
    private final boolean getPathsInNodeBatches;
    
    // Metadata pulling control
    private boolean skipDescendantDocsForSpecificTypes;
    private boolean skipDescendantDocsForSpecificAspects;
    private final Set<QName> typesForSkippingDescendantDocs = new HashSet<>();
    private final Set<QName> aspectsForSkippingDescendantDocs = new HashSet<>();
    private final SOLRAPIClient repositoryClient;
    private final ConcurrentLRUCache<String, Boolean> isIdIndexCache = new ConcurrentLRUCache<>(60*60*100, 60*60*50);
    private final ReentrantReadWriteLock activeTrackerThreadsLock = new ReentrantReadWriteLock();
    private final HashSet<Long> activeTrackerThreads = new HashSet<>();
    private final ReentrantReadWriteLock commitAndRollbackLock = new ReentrantReadWriteLock();
    private final String hostName;
    private final Properties props;
    private final LRU txnIdCache = new LRU(250000);
    private final LRU aclChangeSetCache = new LRU(250000);
    private final Map<Long, Long> cleanContentCache = Collections.synchronizedMap(new LRU(250000));
    private final LRU cleanCascadeCache = new LRU(250000);

    private final int port;
    private final String baseUrl;

    private String skippingDocsQueryString;
    private boolean isSkippingDocsInitialized;

    private final boolean dateFieldDestructuringHasBeenEnabledOnThisInstance;

    static class DocListCollector implements Collector, LeafCollector
    {
        private final IntArrayList docs = new IntArrayList();
        private int docBase;

        public IntArrayList getDocs()
        {
            return this.docs;
        }

        public boolean needsScores()
        {
            return false;
        }

        public LeafCollector getLeafCollector(LeafReaderContext context)
        {
            this.docBase = context.docBase;
            return this;
        }

        public void setScorer(Scorer scorer)
        {
            // Nothing to be done here
        }

        public void collect(int doc)
        {
            docs.add(doc + docBase);
        }
    }

    static class TxnCacheFilter extends DelegatingCollector
    {
        private NumericDocValues currentLongs;
        private final Map<Long, Long> txnLRU;

        TxnCacheFilter(Map<Long, Long> txnLRU)
        {
            this.txnLRU = txnLRU;
        }

        public void doSetNextReader(LeafReaderContext context) throws IOException
        {
            super.doSetNextReader(context);
            currentLongs = context.reader().getNumericDocValues(FIELD_INTXID);
        }

        public void collect(int doc) throws IOException
        {
            long txnId = currentLongs.get(doc);

            if(!txnLRU.containsKey(txnId))
            {
                this.leafDelegate.collect(doc);
            }
        }
    }

    static class TxnCollector extends DelegatingCollector
    {
        private NumericDocValues currentLongs;
        private final LongHashSet txnSet = new LongHashSet(1000);
        private final long txnFloor;
        private final long txnCeil;

        TxnCollector(long txnFloor)
        {
            this.txnFloor = txnFloor;
            this.txnCeil = txnFloor+500;
        }

        @Override
        public void doSetNextReader(LeafReaderContext context) throws IOException
        {
            currentLongs = context.reader().getNumericDocValues(FIELD_INTXID);
        }

        @Override
        public boolean needsScores()
        {
            return false;
        }

        @Override
        public void collect(int doc)
        {
            long txnId = currentLongs.get(doc);
            if(txnId >= txnFloor && txnId < txnCeil)
            {
                txnSet.add(txnId);

            }
        }

        LongHashSet getTxnSet()
        {
            return txnSet;
        }
    }

    static class LRU extends LinkedHashMap<Long,Long>
    {
        private final int maxSize;

        LRU(int maxSize)
        {
            super((int)(maxSize*1.35));
            this.maxSize = maxSize;
        }

        @Override
        protected boolean removeEldestEntry(Map.Entry eldest)
        {
            return size() > this.maxSize;
        }
    }

    @FunctionalInterface
    interface DefinitionExistChecker
    {
        boolean isDefinitionExists(QName qName);
    }

    static abstract class TransactionInfoReporter
    {
        protected final IndexHealthReport report;

        TransactionInfoReporter(IndexHealthReport report)
        {
            this.report = report;
        }

        abstract void reportIdInIndexButNotInDb(long id);
        abstract void reportIdInDbButNotInIndex(long id);
        abstract void reportDuplicatedIdInIndex(long id);
        abstract void reportUniqueIdsInIndex(long count);
    }

    interface SetDuplicatesCommand
    {
        void execute(IndexHealthReport indexHealthReport, long id);
    }

    public SolrInformationServer(AlfrescoCoreAdminHandler adminHandler, SolrCore core, SOLRAPIClient repositoryClient)
    {
        this.adminHandler = adminHandler;
        this.core = core;
        this.nativeRequestHandler = core.getRequestHandler(REQUEST_HANDLER_NATIVE);
        this.cloud = new Cloud();
        this.repositoryClient = Objects.requireNonNull(repositoryClient);

        Properties coreConfiguration = core.getResourceLoader().getCoreProperties();

        contentIndexingHasBeenEnabledOnThisInstance = Boolean.parseBoolean(coreConfiguration.getProperty("alfresco.index.transformContent", "true"));
        LOGGER.info(
                "Content Indexing (AKA Transformation) has been {} on this instance.",
                contentIndexingHasBeenEnabledOnThisInstance ? "enabled" : "disabled");

        recordUnindexedNodes = Boolean.parseBoolean(coreConfiguration.getProperty("alfresco.recordUnindexedNodes", "true"));
        lag = Integer.parseInt(coreConfiguration.getProperty("alfresco.lag", "1000"));
        holeRetention = Integer.parseInt(coreConfiguration.getProperty("alfresco.hole.retention", "3600000"));

        fingerprintHasBeenEnabledOnThisInstance = Boolean.parseBoolean(coreConfiguration.getProperty("alfresco.fingerprint", "true"));
        LOGGER.info(
                "Fingerprint has been {} on this instance.",
                fingerprintHasBeenEnabledOnThisInstance ? "enabled" : "disabled");

        dataModel = AlfrescoSolrDataModel.getInstance();

        contentStreamLimit = Integer.parseInt(coreConfiguration.getProperty("alfresco.contentStreamLimit", "10000000"));
        
        getPathsInNodeBatches = Boolean.parseBoolean(coreConfiguration.getProperty("alfresco.metadata.getPathsInNodeBatches", "true"));

        props = AlfrescoSolrDataModel.getCommonConfig();
        hostName = ConfigUtil.locateProperty(SOLR_HOST, props.getProperty(SOLR_HOST));

        port = portNumber(props);
        baseUrl = baseUrl(props);

        dateFieldDestructuringHasBeenEnabledOnThisInstance = Boolean.parseBoolean(coreConfiguration.getProperty("alfresco.destructureDateFields", "true"));
        LOGGER.info(
                "Date fields destructuring has been {} on this instance.",
                dateFieldDestructuringHasBeenEnabledOnThisInstance ? "enabled" : "disabled");
    }

    @Override
    public AlfrescoCoreAdminHandler getAdminHandler()
    {
        return this.adminHandler;
    }

    @Override
    public boolean cascadeTrackingEnabled()
    {
        return ofNullable((String) props.get(CASCADE_TRACKER_ENABLED))
                    .map(Boolean::parseBoolean)
                    .orElse(true);
    }

    @Override
    public synchronized void initSkippingDescendantDocs()
    {
        if (isSkippingDocsInitialized)
        {
            return;
        }

        Properties p = core.getResourceLoader().getCoreProperties();
        skipDescendantDocsForSpecificTypes = Boolean.parseBoolean(p.getProperty("alfresco.metadata.skipDescendantDocsForSpecificTypes", "false"));
        if (skipDescendantDocsForSpecificTypes)
        {
            initSkippingDescendantDocs(p, typesForSkippingDescendantDocs, PROP_PREFIX_PARENT_TYPE, FIELD_TYPE,
                    qName -> (null != dataModel.getDictionaryService(CMISStrictDictionaryService.DEFAULT).getType(qName)));
        }

        skipDescendantDocsForSpecificAspects = Boolean.parseBoolean(p.getProperty("alfresco.metadata.skipDescendantDocsForSpecificAspects", "false"));
        if (skipDescendantDocsForSpecificAspects)
        {
            initSkippingDescendantDocs(p, aspectsForSkippingDescendantDocs, PROP_PREFIX_PARENT_ASPECT, FIELD_ASPECT,
                    qName -> (null != dataModel.getDictionaryService(CMISStrictDictionaryService.DEFAULT).getAspect(qName)));
        }

        isSkippingDocsInitialized = true;
    }

    /**
     * The outdated node are computed counting all nodes that have
     *
     * <ul>
     *     <li>
     *          LATEST_APPLIED_CONTENT_VERSION_ID = {@link #CONTENT_OUTDATED_MARKER
     *     </li>
     *     <li>
     *         DOC_TYPE = {@link #DOC_TYPE_NODE}
     *     </li>
     * </ul>
     *
     * The updated nodes are computed by simply subtracting the count above from the total number of documents that
     * represent nodes. However, keep in mind that using the two fields LATEST_APPLIED_CONTENT_VERSION_ID and LAST_INCOMING_CONTENT_VERSION_ID
     * there's another (more expensive) way to compute the same information: the following query should return the
     * same result (i.e. number of updated nodes):
     *
     * <pre>
     *   facet.query={!frange key='UPDATED' l=0}sub(LAST_INCOMING_CONTENT_VERSION_ID,LATEST_APPLIED_CONTENT_VERSION_ID)
     * </pre>
     */
    @Override
    public void addContentOutdatedAndUpdatedCounts(NamedList<Object> report)
    {
        try (SolrQueryRequest request = newSolrQueryRequest())
        {
            ModifiableSolrParams params =
                    new ModifiableSolrParams(request.getParams())
                            .set(CommonParams.Q, FIELD_DOC_TYPE + ":" + DOC_TYPE_NODE + " AND " + FIELD_TYPE + ":\"{http://www.alfresco.org/model/content/1.0}content\"")
                            .set(CommonParams.ROWS, 0)
                            .set(FacetParams.FACET, "on")
                            .add(FacetParams.FACET_QUERY, "{!key='OUTDATED'}" + LAST_INCOMING_CONTENT_VERSION_ID + ":\"" + CONTENT_OUTDATED_MARKER + "\"");

            SolrQueryResponse response = cloud.getResponse(nativeRequestHandler, request, params);

            long numFound =
                    ofNullable(response)
                        .map(SolrQueryResponse::getValues)
                        .map(NamedList.class::cast)
                        .map(facets -> facets.get("response"))
                        .map(BasicResultContext.class::cast)
                        .map(BasicResultContext::getDocList)
                        .map(DocList::matches)
                        .orElse(0);

            long outdated =
                    ofNullable(response)
                        .map(SolrQueryResponse::getValues)
                        .map(NamedList.class::cast)
                        .map(facets -> facets.get("facet_counts"))
                        .map(NamedList.class::cast)
                        .map(facetCounts -> facetCounts.get("facet_queries"))
                        .map(NamedList.class::cast)
                        .map(facetQueries -> facetQueries.get("OUTDATED"))
                        .map(Number.class::cast)
                        .map(Number::longValue)
                        .orElse(0L);

            report.add("Node count whose content is in sync", numFound - outdated);
            report.add("Node count whose content needs to be updated", outdated);
        }
    }

    @Override
    public void afterInitModels()
    {
        this.dataModel.afterInitModels();
    }

    @Override
    public AclReport checkAclInIndex(Long aclid, AclReport aclReport)
    {
        String query = FIELD_ACLID + ":" + aclid + AND + FIELD_DOC_TYPE + ":" + DOC_TYPE_ACL;
        long count = this.getDocListSize(query);
        aclReport.setIndexedAclDocCount(count);

        return aclReport;
    }

    @Override
    public IndexHealthReport reportIndexTransactions(Long minTxId, IOpenBitSet txIdsInDb, long maxTxId)
    {
        try (SolrQueryRequest request = newSolrQueryRequest())
        {
            NamedList<Integer> docTypeCounts = this.getFacets(request, "*:*", FIELD_DOC_TYPE, 0);

            // TX
            IndexHealthReport report = new IndexHealthReport(this);
            TransactionInfoReporter txReporter = new TransactionInfoReporter(report)
            {
                @Override
                void reportIdInIndexButNotInDb(long txid)
                {
                    report.setTxInIndexButNotInDb(txid);
                }

                @Override
                void reportIdInDbButNotInIndex(long id)
                {
                    report.setMissingTxFromIndex(id);
                }

                @Override
                void reportDuplicatedIdInIndex(long id)
                {
                    report.setDuplicatedTxInIndex(id);
                }

                @Override
                void reportUniqueIdsInIndex(long count)
                {
                    report.setUniqueTransactionDocsInIndex(count);
                }
            };
            reportTransactionInfo(txReporter, minTxId, maxTxId, txIdsInDb, request, FIELD_TXID);
            long transactionDocsInIndex = getSafeCount(docTypeCounts, DOC_TYPE_TX);
            report.setTransactionDocsInIndex(transactionDocsInIndex);
            report.setDbTransactionCount(txIdsInDb.cardinality());

            // NODE
            setDuplicates(report, request, DOC_TYPE_NODE, IndexHealthReport::setDuplicatedLeafInIndex);
            long leafDocCountInIndex = getSafeCount(docTypeCounts, DOC_TYPE_NODE);
            report.setLeafDocCountInIndex(leafDocCountInIndex);

            // ERROR
            setDuplicates(report, request, DOC_TYPE_ERROR_NODE, IndexHealthReport::setDuplicatedErrorInIndex);
            long errorCount = getSafeCount(docTypeCounts, DOC_TYPE_ERROR_NODE);
            report.setErrorDocCountInIndex(errorCount);

            // UNINDEXED
            setDuplicates(report, request, DOC_TYPE_UNINDEXED_NODE, IndexHealthReport::setDuplicatedUnindexedInIndex);
            long unindexedDocCountInIndex = getSafeCount(docTypeCounts, DOC_TYPE_UNINDEXED_NODE);
            report.setUnindexedDocCountInIndex(unindexedDocCountInIndex);
            return report;
        }
    }

    @Override
    public IndexHealthReport reportAclTransactionsInIndex(Long minAclTxId, IOpenBitSet aclTxIdsInDb, long maxAclTxId)
    {
        try (SolrQueryRequest request = newSolrQueryRequest())
        {
            NamedList<Integer> docTypeCounts = this.getFacets(request, "*:*", FIELD_DOC_TYPE, 0);
            IndexHealthReport report = new IndexHealthReport(this);
            TransactionInfoReporter aclTxReporter = new TransactionInfoReporter(report)
            {
                @Override
                void reportIdInIndexButNotInDb(long txid)
                {
                    report.setAclTxInIndexButNotInDb(txid);
                }

                @Override
                void reportIdInDbButNotInIndex(long id)
                {
                    report.setMissingAclTxFromIndex(id);
                }

                @Override
                void reportDuplicatedIdInIndex(long id)
                {
                    report.setDuplicatedAclTxInIndex(id);
                }

                @Override
                void reportUniqueIdsInIndex(long count)
                {
                    report.setUniqueAclTransactionDocsInIndex(count);
                }
            };
            reportTransactionInfo(aclTxReporter, minAclTxId, maxAclTxId, aclTxIdsInDb, request, FIELD_ACLTXID);
            long aclTransactionDocsInIndex = getSafeCount(docTypeCounts, DOC_TYPE_ACL_TX);
            report.setAclTransactionDocsInIndex(aclTransactionDocsInIndex);
            report.setDbAclTransactionCount(aclTxIdsInDb.cardinality());
            return report;
        }
    }

    @Override
    public List<TenantDbId> getDocsWithUncleanContent() throws IOException
    {
        RefCounted<SolrIndexSearcher> refCounted = null;
        try
        {
            List<TenantDbId> docIds = new ArrayList<>();
            refCounted = this.core.getSearcher();
            SolrIndexSearcher searcher = refCounted.get();

            /*
            *  Below is the code for purging the cleanContentCache.
            *  The cleanContentCache is an in-memory LRU cache of the transactions that have already
            *  had their content fetched. This is needed because the ContentTracker does not have an up-to-date
            *  snapshot of the index to determine which nodes are marked as dirty/new. The cleanContentCache is used
            *  to filter out nodes that belong to transactions that have already been processed, which stops them from
            *  being re-processed.
            *
            *  The cleanContentCache needs to be purged periodically to support retrying of failed content fetches.
            *  This is because fetches for individual nodes within the transaction may have failed, but the transaction will still be in the
            *  cleanContentCache, which prevents it from being retried.
            *
            *  Once a transaction is purged from the cleanContentCache it will be retried automatically if it is marked dirty/new
            *  in current snapshot of the index.
            *
            *  The code below runs every two minutes and purges transactions from the
            *  cleanContentCache that is more than 20 minutes old.
            *
            */
            long purgeTime = System.currentTimeMillis();
            if(purgeTime - cleanContentLastPurged > 120000)
            {
                Iterator<Entry<Long,Long>> entries = cleanContentCache.entrySet().iterator();
                while (entries.hasNext())
                {
                    Entry<Long, Long> entry = entries.next();
                    long txnTime = entry.getValue();
                    if (purgeTime - txnTime > 1200000)
                    {
                        //Purge the clean content cache of records more then 20 minutes old.
                        entries.remove();
                    }
                }
                cleanContentLastPurged = purgeTime;
            }

            long txnFloor;
            Sort sort = new Sort(new SortField(FIELD_INTXID, SortField.Type.LONG));
            sort = sort.rewrite(searcher);
            TopFieldCollector collector = TopFieldCollector.create(sort,
                    1,
                    null,
                    false,
                    false,
                    false);

            DelegatingCollector delegatingCollector = new TxnCacheFilter(cleanContentCache); //Filter transactions that have already been processed.
            delegatingCollector.setLastDelegate(collector);
            searcher.search(documentsWithOutdatedContentQuery(), delegatingCollector);

            LOGGER.debug("{}-[CORE {}] Processing {} documents with content to be indexed", Thread.currentThread().getId(), core.getName(), collector.getTotalHits());


            if(collector.getTotalHits() == 0)
            {
                LOGGER.debug("No documents with outdated text content have been found.");
                return docIds;
            }

            LOGGER.debug("Found {} documents with outdated text content.", collector.getTotalHits());

            ScoreDoc[] scoreDocs = collector.topDocs().scoreDocs;
            List<LeafReaderContext> leaves = searcher.getTopReaderContext().leaves();
            int index = ReaderUtil.subIndex(scoreDocs[0].doc, leaves);
            LeafReaderContext context = leaves.get(index);
            NumericDocValues longs = context.reader().getNumericDocValues(FIELD_INTXID);
            txnFloor = longs.get(scoreDocs[0].doc - context.docBase);

            //Find the next N transactions
            //The TxnCollector collects the transaction ids from the matching documents
            //The txnIds are limited to a range >= the txnFloor and < an arbitrary transaction ceiling.
            TxnCollector txnCollector = new TxnCollector(txnFloor);
            searcher.search(documentsWithOutdatedContentQuery(), txnCollector);
            LongHashSet txnSet = txnCollector.getTxnSet();

            if(txnSet.size() == 0)
            {
                //This should really never be the case, at a minimum the transaction floor should be collected.
                return docIds;
            }

            FieldType fieldType = searcher.getSchema().getField(FIELD_INTXID).getType();
            BooleanQuery.Builder builder = new BooleanQuery.Builder();

            for (LongCursor cursor : txnSet)
            {
                long txnID = cursor.value;
                //Build up the query for the filter of transactions we need to pull the dirty content for.
                TermQuery txnIDQuery = new TermQuery(new Term(FIELD_INTXID, fieldType.readableToIndexed(Long.toString(txnID))));
                builder.add(new BooleanClause(txnIDQuery, BooleanClause.Occur.SHOULD));
            }

            BooleanQuery txnFilterQuery = builder.build();

            //Get the docs with dirty content for the transactions gathered above.
            DocListCollector docListCollector = new DocListCollector();
            BooleanQuery.Builder builder2 = new BooleanQuery.Builder();

            builder2.add(documentsWithOutdatedContentQuery(), BooleanClause.Occur.MUST);
            builder2.add(new QueryWrapperFilter(txnFilterQuery), BooleanClause.Occur.MUST);

            searcher.search(builder2.build(), docListCollector);
            IntArrayList docList = docListCollector.getDocs();
            int size = docList.size();

            List<Long> processedTxns = new ArrayList<>();
            for (int i = 0; i < size; ++i)
            {
                int doc = docList.get(i);
                Document document = searcher.doc(doc, ID_AND_CONTENT_VERSION_ID_AND_CONTENT_LOCALE);
                index = ReaderUtil.subIndex(doc, leaves);
                context = leaves.get(index);
                longs = context.reader().getNumericDocValues(FIELD_INTXID);

                long txnId = longs.get(doc - context.docBase);

                if(!cleanContentCache.containsKey(txnId))
                {
                    processedTxns.add(txnId);
                    IndexableField id = document.getField(FIELD_SOLR4_ID);
                    String idString = id.stringValue();
                    TenantDbId tenantAndDbId = AlfrescoSolrDataModel.decodeNodeDocumentId(idString);

                    ofNullable(document.getField(CONTENT_LOCALE_FIELD))
                            .map(IndexableField::stringValue)
                            .ifPresent(value -> tenantAndDbId.setProperty(CONTENT_LOCALE_FIELD, value));

                    tenantAndDbId.setProperty(
                            LATEST_APPLIED_CONTENT_VERSION_ID,
                            ofNullable(document.getField(LATEST_APPLIED_CONTENT_VERSION_ID))
                                    .map(IndexableField::stringValue)
                                    .orElse(null));
                    docIds.add(tenantAndDbId);
                }
            }

            long txnTime = System.currentTimeMillis();

            for(Long l : processedTxns)
            {
                //Save the indexVersion so we know when we can clean out this entry
                cleanContentCache.put(l, txnTime);
            }

            return docIds;
        }
        finally
        {
            ofNullable(refCounted).ifPresent(RefCounted::decref);
        }
    }

    @Override
    public void addCommonNodeReportInfo(NodeReport nodeReport)
    {
        long dbId = nodeReport.getDbid();
        String query = FIELD_DBID + ":" + dbId + AND + FIELD_DOC_TYPE + ":" + DOC_TYPE_NODE;
        long count = this.getDocListSize(query);
        nodeReport.setIndexedNodeDocCount(count);
    }

    @Override
    public void commit() throws IOException
    {
        // avoid multiple commits and warming searchers
        commitAndRollbackLock.writeLock().lock();
        try
        {
            canUpdate();
            UpdateRequestProcessor processor = null;
            try (SolrQueryRequest request = newSolrQueryRequest())
            {
                processor = this.core.getUpdateProcessingChain(null).createProcessor(request, newSolrQueryResponse());
                processor.processCommit(new CommitUpdateCommand(request, false));
            }
            finally
            {
                if (processor != null)
                {
                    processor.finish();
                }
            }
        }
        finally
        {
            commitAndRollbackLock.writeLock().unlock();
        }
    }

    @Override
    public void hardCommit() throws IOException
    {
        // avoid multiple commits and warming searchers
        commitAndRollbackLock.writeLock().lock();
        try
        {
            UpdateRequestProcessor processor = null;
            try (SolrQueryRequest request = newSolrQueryRequest())
            {
                processor = this.core.getUpdateProcessingChain(null).createProcessor(request, newSolrQueryResponse());
                CommitUpdateCommand commitUpdateCommand = new CommitUpdateCommand(request, false);
                commitUpdateCommand.openSearcher = false;
                commitUpdateCommand.softCommit = false;
                commitUpdateCommand.waitSearcher = false;
                processor.processCommit(commitUpdateCommand);
            }
            finally
            {
                if (processor != null)
                {
                    processor.finish();
                }
            }
        }
        finally
        {
            commitAndRollbackLock.writeLock().unlock();
        }
    }

    @Override
    public boolean commit(boolean openSearcher) throws IOException
    {
        canUpdate();

        UpdateRequestProcessor processor = null;
        boolean searcherOpened = false;
        try (SolrQueryRequest request = newSolrQueryRequest())
        {
            processor = this.core.getUpdateProcessingChain(null).createProcessor(request, newSolrQueryResponse());
            CommitUpdateCommand command = new CommitUpdateCommand(request, false);
            if (openSearcher)
            {
                RefCounted<SolrIndexSearcher> active = null;
                RefCounted<SolrIndexSearcher> newest = null;
                try
                {
                    active = core.getSearcher();
                    newest = core.getNewestSearcher(false);
                    if (active.get() == newest.get())
                    {
                        searcherOpened = command.openSearcher = true;
                        command.waitSearcher = false;
                    }
                    else
                    {
                        searcherOpened = command.openSearcher = false;
                    }
                }
                finally
                {
                    ofNullable(active).ifPresent(RefCounted::decref);
                    ofNullable(newest).ifPresent(RefCounted::decref);
                }
            }
            processor.processCommit(command);
        }
        finally
        {
            if (processor != null)
            {
                processor.finish();
            }
        }

        return searcherOpened;
    }

    @Override
    public void deleteByAclChangeSetId(Long aclChangeSetId) throws IOException
    {
        deleteById(FIELD_INACLTXID, aclChangeSetId);
    }

    @Override
    public void deleteByAclId(Long aclId) throws IOException
    {
        isIdIndexCache.clear();
        deleteById(FIELD_ACLID, aclId);
    }

    @Override
    public void deleteByNodeId(Long nodeId) throws IOException
    {
        deleteById(FIELD_DBID, nodeId);
    }

    @Override
    public void deleteByTransactionId(Long transactionId) throws IOException
    {
        isIdIndexCache.clear();
        deleteById(FIELD_INTXID, transactionId);
    }

    @Override
    public List<AlfrescoModel> getAlfrescoModels()
    {
        return dataModel.getAlfrescoModels();
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public Iterable<Entry<String, Object>> getCoreStats() throws IOException
    {
        // This is still local, not totally cloud-friendly
        // TODO Make this cloud-friendly by aggregating the stats across the cloud

        NamedList<Object> coreSummary = new SimpleOrderedMap<>();
        RefCounted<SolrIndexSearcher> refCounted = null;
        try (SolrQueryRequest request = newSolrQueryRequest())
        {
            NamedList docTypeCounts = this.getFacets(request, "*:*", FIELD_DOC_TYPE, 0);
            long aclCount = getSafeCount(docTypeCounts, DOC_TYPE_ACL);
            coreSummary.add("Alfresco Acls in Index", aclCount);
            long nodeCount = getSafeCount(docTypeCounts, DOC_TYPE_NODE);
            coreSummary.add("Alfresco Nodes in Index", nodeCount);
            long txCount = getSafeCount(docTypeCounts, DOC_TYPE_TX);
            coreSummary.add("Alfresco Transactions in Index", txCount);
            long aclTxCount = getSafeCount(docTypeCounts, DOC_TYPE_ACL_TX);
            coreSummary.add("Alfresco Acl Transactions in Index", aclTxCount);
            long stateCount = getSafeCount(docTypeCounts, DOC_TYPE_STATE);
            coreSummary.add("Alfresco States in Index", stateCount);
            long unindexedNodeCount = getSafeCount(docTypeCounts, DOC_TYPE_UNINDEXED_NODE);
            coreSummary.add("Alfresco Unindexed Nodes", unindexedNodeCount);
            long errorNodeCount = getSafeCount(docTypeCounts, DOC_TYPE_ERROR_NODE);
            coreSummary.add("Alfresco Error Nodes in Index", errorNodeCount);

            refCounted = core.getSearcher(false, true, null);
            SolrIndexSearcher solrIndexSearcher = refCounted.get();
            coreSummary.add("Searcher", solrIndexSearcher.getStatistics());
            Map<String, SolrInfoMBean> infoRegistry = core.getInfoRegistry();
            for (Entry<String, SolrInfoMBean> infos : infoRegistry.entrySet())
            {
                SolrInfoMBean infoMBean = infos.getValue();
                String key = infos.getKey();
                if (key.equals("/alfresco"))
                {
                    coreSummary.add("/alfresco", fixStats(infoMBean.getStatistics()));
                }

                if (key.equals("/afts"))
                {
                    coreSummary.add("/afts", fixStats(infoMBean.getStatistics()));
                }

                if (key.equals("/cmis"))
                {
                    coreSummary.add("/cmis", fixStats(infoMBean.getStatistics()));
                }

                if (key.equals("filterCache"))
                {
                    coreSummary.add("/filterCache", infoMBean.getStatistics());
                }

                if (key.equals("queryResultCache"))
                {
                    coreSummary.add("/queryResultCache", infoMBean.getStatistics());
                }

                if (key.equals("alfrescoAuthorityCache"))
                {
                    coreSummary.add("/alfrescoAuthorityCache", infoMBean.getStatistics());
                }

                if (key.equals("alfrescoPathCache"))
                {
                    coreSummary.add("/alfrescoPathCache", infoMBean.getStatistics());
                }
            }

            // Adds detailed stats for each registered searcher
            int searcherIndex = 0;
            List<SolrIndexSearcher> searchers = getRegisteredSearchers();
            for (SolrIndexSearcher searcher : searchers)
            {
                NamedList<Object> details = new SimpleOrderedMap<>();
                details.add("Searcher", searcher.getStatistics());
                coreSummary.add("Searcher-" + searcherIndex, details);
                searcherIndex++;
            }

            coreSummary.add("Number of Searchers", searchers.size());
            // This is zero for Solr4, whereas we had some local caches before
            coreSummary.add("Total Searcher Cache (GB)", 0);

            IndexDeletionPolicyWrapper delPolicy = core.getDeletionPolicy();
            IndexCommit indexCommit = delPolicy.getLatestCommit();
            // race?
            if (indexCommit == null)
            {
                indexCommit = solrIndexSearcher.getIndexReader().getIndexCommit();
            }

            if (indexCommit != null)
            {
                // Tells Solr to stop deleting things for 20 seconds so we can get a snapshot of all the files on the index
                delPolicy.setReserveDuration(solrIndexSearcher.getIndexReader().getVersion(), 20000);
                long fileSize = 0L;

                File dir = new File(solrIndexSearcher.getPath());
                for (String name : indexCommit.getFileNames())
                {
                    File file = new File(dir, name);
                    if (file.exists())
                    {
                        fileSize += file.length();
                    }
                }

                DecimalFormat df = new DecimalFormat("###,###.######");
                coreSummary.add("On disk (GB)", df.format(fileSize / 1024.0f / 1024.0f / 1024.0f));
                coreSummary.add("Per node B", nodeCount > 0 ? fileSize / nodeCount : 0);
            }
        }
        finally
        {
            ofNullable(refCounted).ifPresent(RefCounted::decref);
        }

        return coreSummary;
    }

    @Override
    public DictionaryComponent getDictionaryService(String alternativeDictionary)
    {
        return this.dataModel.getDictionaryService(alternativeDictionary);
    }

    @Override
    public int getTxDocsSize(String targetTxId, String targetTxCommitTime)
    {
        return getDocListSize(FIELD_TXID + ":" + targetTxId + AND + FIELD_TXCOMMITTIME + ":" + targetTxCommitTime);
    }

    @Override
    public int getAclTxDocsSize(String aclTxId, String aclTxCommitTime)
    {
        return getDocListSize(FIELD_ACLTXID + ":" + aclTxId + AND + FIELD_ACLTXCOMMITTIME + ":" + aclTxCommitTime);
    }

    @Override
    public Set<Long> getErrorDocIds() throws IOException
    {
        Set<Long> errorDocIds = new HashSet<>();
        RefCounted<SolrIndexSearcher> refCounted = null;
        try
        {
            refCounted = this.core.getSearcher();
            SolrIndexSearcher searcher = refCounted.get();
            TermQuery errorQuery = new TermQuery(new Term(FIELD_DOC_TYPE, DOC_TYPE_ERROR_NODE));
            DocListCollector docListCollector = new DocListCollector();
            searcher.search(errorQuery, docListCollector);
            IntArrayList docList = docListCollector.getDocs();
            int size = docList.size();

            for (int i = 0; i < size; ++i)
            {
                int doc = docList.get(i);
                Document document = searcher.doc(doc, REQUEST_ONLY_ID_FIELD);
                IndexableField id = document.getField(FIELD_SOLR4_ID);
                String idString = id.stringValue();

                if (idString.startsWith(PREFIX_ERROR))
                {
                    idString = idString.substring(PREFIX_ERROR.length());
                }

                errorDocIds.add(Long.valueOf(idString));
            }
        }
        finally
        {
            ofNullable(refCounted).ifPresent(RefCounted::decref);
        }
        return errorDocIds;
    }

    @Override
    public long getHoleRetention()
    {
        return this.holeRetention;
    }

    @Override
    public M2Model getM2Model(QName modelQName)
    {
        return this.dataModel.getM2Model(modelQName);
    }

    @Override
    public Map<String, Set<String>> getModelErrors()
    {
        return dataModel.getModelErrors();
    }

    @Override
    public NamespaceDAO getNamespaceDAO()
    {
        return this.dataModel.getNamespaceDAO();
    }

    @Override
    public IOpenBitSet getOpenBitSetInstance()
    {
        return new SolrOpenBitSetAdapter();
    }

    @Override
    public int getRegisteredSearcherCount()
    {
        return getRegisteredSearchers().size();
    }

    @Override
    public <T> ISimpleOrderedMap<T> getSimpleOrderedMapInstance()
    {
        return new SolrSimpleOrderedMap<>();
    }

    @Override
    public TrackerStats getTrackerStats()
    {
        return this.trackerStats;
    }

    @Override
    public TrackerState getTrackerInitialState()
    {
        try (SolrQueryRequest request = newSolrQueryRequest())
        {
            TrackerState state = new TrackerState();
            SolrRequestHandler handler = core.getRequestHandler(REQUEST_HANDLER_GET);

            ModifiableSolrParams newParams =
                    new ModifiableSolrParams(request.getParams())
                            .set("ids", "TRACKER!STATE!ACLTX,TRACKER!STATE!TX");
            request.setParams(newParams);

            SolrDocumentList response = executeQueryRequest(request, newSolrQueryResponse(), handler);
            if (response == null)
            {
                LOGGER.error("Got no response from a tracker initial state request.");
                return state;
            }

            for (int i = 0; i < response.getNumFound(); i++)
            {
                SolrDocument current = response.get(i);
                // ACLTX
                if (current.getFieldValue(FIELD_S_ACLTXCOMMITTIME) != null)
                {
                    if (state.getLastIndexedChangeSetCommitTime() == 0)
                    {
                        state.setLastIndexedChangeSetCommitTime(getFieldValueLong(current, FIELD_S_ACLTXCOMMITTIME));
                    }

                    if (state.getLastIndexedChangeSetId() == 0)
                    {
                        state.setLastIndexedChangeSetId(getFieldValueLong(current, FIELD_S_ACLTXID));
                    }
                }

                // TX
                if (current.getFieldValue(FIELD_S_TXCOMMITTIME) != null)
                {
                    if (state.getLastIndexedTxCommitTime() == 0)
                    {
                        state.setLastIndexedTxCommitTime(getFieldValueLong(current, FIELD_S_TXCOMMITTIME));
                    }

                    if (state.getLastIndexedTxId() == 0)
                    {
                        state.setLastIndexedTxId(getFieldValueLong(current, FIELD_S_TXID));
                    }
                }
            }

            long startTime = System.currentTimeMillis();
            state.setLastStartTime(startTime);
            state.setTimeToStopIndexing(startTime - lag);
            state.setTimeBeforeWhichThereCanBeNoHoles(startTime - holeRetention);

            long timeBeforeWhichThereCanBeNoTxHolesInIndex = state.getLastIndexedTxCommitTime() - holeRetention;
            state.setLastGoodTxCommitTimeInIndex(timeBeforeWhichThereCanBeNoTxHolesInIndex > 0 ? timeBeforeWhichThereCanBeNoTxHolesInIndex : 0);

            long timeBeforeWhichThereCanBeNoChangeSetHolesInIndex = state.getLastIndexedChangeSetCommitTime() - holeRetention;
            state.setLastGoodChangeSetCommitTimeInIndex(timeBeforeWhichThereCanBeNoChangeSetHolesInIndex > 0 ? timeBeforeWhichThereCanBeNoChangeSetHolesInIndex : 0);

            LOGGER.debug("The tracker initial state was created: " + state);

            return state;
        }
    }

    @Override
    public void continueState(TrackerState state)
    {
        long startTime = System.currentTimeMillis();
        long lastStartTime = state.getLastStartTime();
        state.setTimeToStopIndexing(startTime - lag);
        state.setTimeBeforeWhichThereCanBeNoHoles(startTime - holeRetention);

        long timeBeforeWhichThereCanBeNoTxHolesInIndex = state.getLastIndexedTxCommitTime() - holeRetention;
        long lastStartTimeWhichThereCanBeNoTxHolesInIndex = lastStartTime - holeRetention;

        /*
        * Choose the max between the last commit time in the index and the last time the tracker started.
        * Hole retention is applied to both.
        *
        * This logic is very tricky and very important to understand.
        *
        * state.getLastGoodTxCommitTimeInIndex() is used to determine where to start pulling transactions from the repo on the
        * current tracker run.
        *
        * If we simply take the current value of  state.getLastIndexedTxCommitTime() we have the following problem:
        *
        * If no data is added to the repo for a long period of time state.getLastIndexedTxCommitTime() never moves forward. This causes the
        * loop inside MetadataTracker.getSomeTransactions() to hammer the repo as the time between state.getLastIndexedTxCommitTime()
        * and state.setTimeToStopIndexing increases.
        *
        * To resolve this we choose the max between the last commit time in the index and the last time the tracker started. In theory
        * if we start looking for transactions after the last tracker was started (and apply hole retention), we should never miss a
        * transaction. Or atleast ensure that principal behind hole retention is respected. This theory should be closely looked at if
        * the trackers ever lose data.
        */

        timeBeforeWhichThereCanBeNoTxHolesInIndex = Math.max(timeBeforeWhichThereCanBeNoTxHolesInIndex, lastStartTimeWhichThereCanBeNoTxHolesInIndex);

        state.setLastGoodTxCommitTimeInIndex(timeBeforeWhichThereCanBeNoTxHolesInIndex > 0 ? timeBeforeWhichThereCanBeNoTxHolesInIndex : 0);

        long timeBeforeWhichThereCanBeNoChangeSetHolesInIndex = state.getLastIndexedChangeSetCommitTime() - holeRetention;
        long lastStartTimeWhichThereCanBeNoChangeSetHolesInIndex = lastStartTime - holeRetention;

        timeBeforeWhichThereCanBeNoChangeSetHolesInIndex = Math.max(timeBeforeWhichThereCanBeNoChangeSetHolesInIndex, lastStartTimeWhichThereCanBeNoChangeSetHolesInIndex);
        state.setLastGoodChangeSetCommitTimeInIndex(timeBeforeWhichThereCanBeNoChangeSetHolesInIndex > 0 ? timeBeforeWhichThereCanBeNoChangeSetHolesInIndex : 0);

        state.setLastStartTime(startTime);
    }

    @Override
    public long indexAcl(List<AclReaders> aclReaderList, boolean overwrite) throws IOException
    {
        long start = System.nanoTime();

        UpdateRequestProcessor processor = null;
        try (SolrQueryRequest request = newSolrQueryRequest())
        {
            processor = this.core.getUpdateProcessingChain(null).createProcessor(request, newSolrQueryResponse());
            for (AclReaders aclReaders : notNullOrEmpty(aclReaderList))
            {
                SolrInputDocument acl = new SolrInputDocument();

                acl.addField(FIELD_SOLR4_ID, getAclDocumentId(aclReaders.getTenantDomain(), aclReaders.getId()));
                acl.addField(FIELD_VERSION, "0");
                acl.addField(FIELD_ACLID, aclReaders.getId());
                acl.addField(FIELD_INACLTXID, aclReaders.getAclChangeSetId());

                String tenant = aclReaders.getTenantDomain();
                for (String reader : notNullOrEmpty(aclReaders.getReaders()))
                {
                    reader = addTenantToAuthority(reader, tenant);
                    acl.addField(FIELD_READER, reader);
                }

                for (String denied : aclReaders.getDenied())
                {
                    denied = addTenantToAuthority(denied, tenant);
                    acl.addField(FIELD_DENIED, denied);
                }
                acl.addField(FIELD_DOC_TYPE, DOC_TYPE_ACL);

                AddUpdateCommand cmd = new AddUpdateCommand(request);
                cmd.overwrite = overwrite;
                cmd.solrDoc = acl;
                processor.processAdd(cmd);
            }
        }
        finally
        {
            if (processor != null) processor.finish();
        }

        return (System.nanoTime() - start);
    }

    @Override
    public void indexAclTransaction(AclChangeSet changeSet, boolean overwrite) throws IOException
    {
        canUpdate();
        UpdateRequestProcessor processor = null;
        try (SolrQueryRequest request = newSolrQueryRequest())
        {
            processor = this.core.getUpdateProcessingChain(null).createProcessor(request, newSolrQueryResponse());

            SolrInputDocument aclTx = new SolrInputDocument();
            aclTx.addField(FIELD_SOLR4_ID, getAclChangeSetDocumentId(changeSet.getId()));
            aclTx.addField(FIELD_VERSION, "0");
            aclTx.addField(FIELD_ACLTXID, changeSet.getId());
            aclTx.addField(FIELD_INACLTXID, changeSet.getId());
            aclTx.addField(FIELD_ACLTXCOMMITTIME, changeSet.getCommitTimeMs());
            aclTx.addField(FIELD_DOC_TYPE, DOC_TYPE_ACL_TX);

            AddUpdateCommand cmd = new AddUpdateCommand(request);
            cmd.overwrite = overwrite;
            cmd.solrDoc = aclTx;
            processor.processAdd(cmd);

            putAclTransactionState(processor, request, changeSet);
        }
        finally
        {
            if (processor != null) processor.finish();
        }
    }

    @Override
    public AclChangeSet getMaxAclChangeSetIdAndCommitTimeInIndex()
    {
        try (SolrQueryRequest request = newSolrQueryRequest())
        {
            SolrDocument aclState = getState(core, request, "TRACKER!STATE!ACLTX");
            if (aclState != null)
            {
                long id = this.getFieldValueLong(aclState, FIELD_S_ACLTXID);
                long commitTime = this.getFieldValueLong(aclState, FIELD_S_ACLTXCOMMITTIME);
                int aclCount = -1; // Irrelevant for this method
                return new AclChangeSet(id, commitTime, aclCount);
            }
            return new AclChangeSet(0, 0, -1);
        }
    }

    @Override
    public void dirtyTransaction(long txnId)
    {
        this.cleanContentCache.remove(txnId);
        if (cascadeTrackingEnabled())
        {
            this.cleanCascadeCache.remove(txnId);
        }
    }

    @Override
    public void capIndex(long dbid) throws IOException
    {
        UpdateRequestProcessor processor = null;
        try (SolrQueryRequest request = newSolrQueryRequest())
        {
            processor = this.core.getUpdateProcessingChain(null).createProcessor(request, newSolrQueryResponse());

            SolrInputDocument input = new SolrInputDocument();
            input.addField(FIELD_SOLR4_ID, INDEX_CAP_ID);
            input.addField(FIELD_VERSION, 0);
            input.addField(FIELD_DBID, -dbid); //Making this negative to ensure it is never confused with node DBID
            input.addField(FIELD_DOC_TYPE, DOC_TYPE_STATE);

            AddUpdateCommand cmd = new AddUpdateCommand(request);
            cmd.overwrite = true;
            cmd.solrDoc = input;
            processor.processAdd(cmd);
        }
        finally
        {
            if (processor != null) processor.finish();
        }
    }

    @Override
    public void maintainCap(long dbid) throws IOException
    {
        deleteByQuery(FIELD_DBID + ":{" + dbid + " TO *}");
    }

    @Override
    public long nodeCount()
    {
        return getDocListSize(FIELD_DOC_TYPE + ":" + DOC_TYPE_NODE);
    }

    @Override
    public long maxNodeId()
    {
        return topNodeId(SolrQuery.ORDER.desc);
    }

    @Override
    public long minNodeId()
    {
        return topNodeId(SolrQuery.ORDER.asc);
    }

    @Override
    public long getIndexCap()
    {
        try (SolrQueryRequest request = this.newSolrQueryRequest())
        {
            ModifiableSolrParams params =
                    new ModifiableSolrParams(request.getParams())
                            .set(CommonParams.Q, FIELD_SOLR4_ID + ":" + INDEX_CAP_ID)
                            .set(CommonParams.ROWS, 1)
                            .set(CommonParams.FL, FIELD_DBID);

            SolrDocumentList docs = cloud.getSolrDocumentList(nativeRequestHandler, request, params);

            return docs.stream()
                    .findFirst()
                    .map(doc -> getFieldValueLong(doc, FIELD_DBID))
                    .map(Math::abs)
                    .orElse(-1L);
        }
    }

    @Override
    public void indexNode(Node node, boolean overwrite) throws IOException, JSONException
    {
        long start = System.nanoTime();
        final SolrQueryRequest request = newSolrQueryRequest();

        UpdateRequestProcessor processor = null;
        try
        {
            processor = this.core.getUpdateProcessingChain(null).createProcessor(request, newSolrQueryResponse());

            LOGGER.debug("Incoming Node {} with Status {}", node.getId(), node.getStatus());

            if ((node.getStatus() == SolrApiNodeStatus.DELETED)
                    || (node.getStatus() == SolrApiNodeStatus.UNKNOWN)
                    || cascadeTrackingEnabled() && ((node.getStatus() == SolrApiNodeStatus.NON_SHARD_DELETED)
                                                 || (node.getStatus() == SolrApiNodeStatus.NON_SHARD_UPDATED)))
            {
                deleteNode(processor, request, node);
            }

            if (node.getStatus() == SolrApiNodeStatus.UPDATED
                    || node.getStatus() == SolrApiNodeStatus.UNKNOWN
                    || (cascadeTrackingEnabled() && node.getStatus() == SolrApiNodeStatus.NON_SHARD_UPDATED))
            {
                LOGGER.debug("Node {} is being updated", node.getId());

                NodeMetaDataParameters nmdp = new NodeMetaDataParameters();
                nmdp.setFromNodeId(node.getId());
                nmdp.setToNodeId(node.getId());
                nmdp.setMaxResults(Integer.MAX_VALUE);

                Optional<Collection<NodeMetaData>> nodeMetaDatas = getNodesMetaDataFromRepository(nmdp);

                if (nodeMetaDatas.isEmpty() || nodeMetaDatas.get().isEmpty())
                {
                    // Using exception for flow handling to jump to error node processing.
                    throw new Exception("Error loading node metadata from repository.");
                }

                NodeMetaData nodeMetaData = nodeMetaDatas.get().iterator().next();
                if (node.getTxnId() == Long.MAX_VALUE)
                {
                    LOGGER.debug("Node {} index request is part of a re-index.", node.getId());
                    this.cleanContentCache.remove(nodeMetaData.getTxnId());
                }

                if (node.getStatus() == SolrApiNodeStatus.UPDATED || node.getStatus() == SolrApiNodeStatus.UNKNOWN)
                {
                    AddUpdateCommand addDocCmd = new AddUpdateCommand(request);
                    addDocCmd.overwrite = overwrite;

                    deleteErrorNode(processor, request, node);

                    // Check index control
                    Map<QName, PropertyValue> properties = nodeMetaData.getProperties();
                    StringPropertyValue pValue = (StringPropertyValue) properties.get(ContentModel.PROP_IS_INDEXED);
                    boolean isIndexed = ofNullable(pValue)
                                            .map(StringPropertyValue::getValue)
                                            .map(Boolean::parseBoolean)
                                            .orElse(true);
                    
                    addDocCmd.solrDoc = isIndexed
                                ? populateWithMetadata(
                                            basicDocument(nodeMetaData, DOC_TYPE_NODE, PartialSolrInputDocument::new),
                                            nodeMetaData, nmdp)
                                : (recordUnindexedNodes
                                            ? basicDocument(nodeMetaData, DOC_TYPE_UNINDEXED_NODE, SolrInputDocument::new)
                                            : null);

                    // UnindexedNodes are not indexed when solrcore property flag "recordUnindexedNodes" is set to false
                    if (addDocCmd != null)
                    {
                        processor.processAdd(addDocCmd);
                    }
                    
                }
            }
        }
        catch (Exception exception)
        {
            LOGGER.error("Node {} index failed and skipped in Tx {}. " +
                    "See the stacktrace below for further details.",
                    node.getId(),
                    node.getTxnId(),
                    exception);

            processor =
                    ofNullable(processor)
                        .orElseGet(() -> this.core.getUpdateProcessingChain(null).createProcessor(request, newSolrQueryResponse()));

            AddUpdateCommand addDocCmd = new AddUpdateCommand(request);
            addDocCmd.overwrite = overwrite;

            SolrInputDocument errorNodeDocument = new SolrInputDocument();
            errorNodeDocument.addField(FIELD_SOLR4_ID, PREFIX_ERROR + node.getId());
            errorNodeDocument.addField(FIELD_VERSION, "0");
            errorNodeDocument.addField(FIELD_DBID, node.getId());
            errorNodeDocument.addField(FIELD_INTXID, node.getTxnId());
            errorNodeDocument.addField(FIELD_EXCEPTION_MESSAGE, exception.getMessage());
            errorNodeDocument.addField(FIELD_DOC_TYPE, DOC_TYPE_ERROR_NODE);

            StringWriter stringWriter = new StringWriter(4096);
            try (PrintWriter printWriter = new PrintWriter(stringWriter, true))
            {
                exception.printStackTrace(printWriter);
                String stack = stringWriter.toString();
                errorNodeDocument.addField(FIELD_EXCEPTION_STACK, stack.length() < 32766 ? stack : stack.substring(0, 32765));
            }

            addDocCmd.solrDoc = errorNodeDocument;
            processor.processAdd(addDocCmd);
        }
        finally
        {
            if (processor != null) processor.finish();
            if (request != null) request.close();

            this.trackerStats.addNodeTime(System.nanoTime() - start);
        }
    }

    @Override
    public List<NodeMetaData> getCascadeNodes(List<Long> txnIds) throws IOException, JSONException
    {
        List<FieldInstance> list = dataModel.getIndexedFieldNamesForProperty(ContentModel.PROP_CASCADE_TX).getFields();
        FieldInstance fieldInstance = list.get(0);

        RefCounted<SolrIndexSearcher> refCounted = null;
        IntArrayList docList;
        Set<Long> parentNodesId = new HashSet<>();

        try
        {
            refCounted = core.getSearcher();
            SolrIndexSearcher searcher = refCounted.get();
            String field = fieldInstance.getField();
            SchemaField schemaField = searcher.getSchema().getField(field);
            FieldType fieldType = schemaField.getType();
            BooleanQuery.Builder builder = new BooleanQuery.Builder();
            BooleanQuery booleanQuery;

            for(Long l : txnIds)
            {
                BytesRefBuilder bytesRefBuilder = new BytesRefBuilder();
                fieldType.readableToIndexed(l.toString(), bytesRefBuilder);
                TermQuery termQuery = new TermQuery(new Term(field, bytesRefBuilder.toBytesRef()));
                BooleanClause booleanClause = new BooleanClause(termQuery, BooleanClause.Occur.SHOULD);
                builder.add(booleanClause);
            }

            booleanQuery = builder.build();

            DocListCollector collector = new DocListCollector();
            searcher.search(booleanQuery, collector);
            docList = collector.getDocs();
            int size = docList.size();
            for(int i=0; i<size; i++)
            {
                int docId = docList.get(i);
                Document document = searcher.doc(docId, REQUEST_ONLY_ID_FIELD);
                IndexableField indexableField = document.getField(FIELD_SOLR4_ID);
                String id = indexableField.stringValue();
                TenantDbId ids = AlfrescoSolrDataModel.decodeNodeDocumentId(id);
                parentNodesId.add(ids.dbId);
            }
        }
        finally
        {
            ofNullable(refCounted).ifPresent(RefCounted::decref);
        }

        List<NodeMetaData> allNodeMetaDatas = new ArrayList<>();

        for (Long parentNodeId : parentNodesId)
        {
            NodeMetaDataParameters nmdp = new NodeMetaDataParameters();
            nmdp.setFromNodeId(parentNodeId);
            nmdp.setToNodeId(parentNodeId);
            nmdp.setIncludeAclId(true);
            nmdp.setIncludeChildAssociations(false);
            nmdp.setIncludeChildIds(true);
            nmdp.setIncludeOwner(false);
            nmdp.setIncludeParentAssociations(false);
            nmdp.setIncludePaths(true);
            nmdp.setIncludeProperties(false);
            nmdp.setIncludeTxnId(true);
            nmdp.setMaxResults(1);
            // Gets only one
            Optional<Collection<NodeMetaData>> nodeMetaDatas = getNodesMetaDataFromRepository(nmdp);
            allNodeMetaDatas.addAll(nodeMetaDatas.orElse(Collections.emptyList()));
        }

        return allNodeMetaDatas;
    }

    @Override
    public void cascadeNodes(List<NodeMetaData> nodeMetaDatas, boolean overwrite) throws IOException, JSONException
    {
        UpdateRequestProcessor processor = null;
        try (SolrQueryRequest request = newSolrQueryRequest())
        {
            processor = this.core.getUpdateProcessingChain(null).createProcessor(request, newSolrQueryResponse());
            for (NodeMetaData nodeMetaData : nodeMetaDatas)
            {
                if (mayHaveChildren(nodeMetaData))
                {
                    cascadeUpdateV2(nodeMetaData, overwrite, request, processor);
                }
            }
        }
        catch (Exception exception)
        {
            LOGGER.error("Exception while processing cascading updates from the parent nodes. " +
                    "See the stacktrace below for further details.",
                    exception);
        }
        finally
        {
            if (processor != null)
            {
                processor.finish();
            }
        }
    }

    @Override
    public void updateContent(TenantDbId docRef) throws Exception
    {
        LOGGER.debug("Text content of Document DBID={} is going to be updated.", docRef.dbId);

        UpdateRequestProcessor processor = null;
        try (SolrQueryRequest request = newSolrQueryRequest())
        {
            processor = this.core.getUpdateProcessingChain(null).createProcessor(request, newSolrQueryResponse());

            SolrInputDocument doc = new PartialSolrInputDocument();
            doc.removeField(FIELD_DBID);
            doc.addField(FIELD_DBID, docRef.dbId);
            doc.setField(FIELD_SOLR4_ID,
                    AlfrescoSolrDataModel.getNodeDocumentId(
                            docRef.tenant,
                            docRef.dbId));

            if (docRef.optionalBag.containsKey(CONTENT_LOCALE_FIELD))
            {
                addContentToDoc(docRef, doc, docRef.dbId);
            }

            LOGGER.debug("Text content of Document DBID={} has been updated (not yet indexed)", docRef.dbId);

            final Long latestAppliedVersionId =
                        ofNullable(docRef.optionalBag.get(LATEST_APPLIED_CONTENT_VERSION_ID))
                                .map(String.class::cast)
                                .map(Long::parseLong)
                                .orElse(CONTENT_UPDATED_MARKER);

            markAsContentInSynch(doc, latestAppliedVersionId);

            // Add to index
            AddUpdateCommand addDocCmd = new AddUpdateCommand(request);
            addDocCmd.overwrite = true;
            addDocCmd.solrDoc = doc;

            processor.processAdd(addDocCmd);

            LOGGER.debug(
                    "Text content of Document DBID={} has been marked as updated (latest content version ID = {})",
                    docRef.dbId,
                    (latestAppliedVersionId == CONTENT_UPDATED_MARKER ? "N.A." : latestAppliedVersionId));
        }
        catch (Exception exception)
        {
            LOGGER.error("Unable to update the text content of node {}. See the stacktrace below for further details.", docRef.dbId, exception);
        }
        finally
        {
            if(processor != null) {processor.finish();}
        }
    }


    @Override
    public void indexNodes(List<Node> nodes, boolean overwrite) throws IOException, JSONException
    {
        UpdateRequestProcessor processor = null;
        try (SolrQueryRequest request = newSolrQueryRequest())
        {
            processor = this.core.getUpdateProcessingChain(null).createProcessor(request, newSolrQueryResponse());

            Map<Long, Node> nodeIdsToNodes = new HashMap<>();
            EnumMap<SolrApiNodeStatus, List<Long>> nodeStatusToNodeIds = new EnumMap<>(SolrApiNodeStatus.class);

            categorizeNodes(nodes, nodeIdsToNodes, nodeStatusToNodeIds);

            List<Long> deletedNodeIds = notNullOrEmpty(nodeStatusToNodeIds.get(SolrApiNodeStatus.DELETED));
            List<Long> shardDeletedNodeIds = Collections.emptyList();
            List<Long> shardUpdatedNodeIds = Collections.emptyList();
            if (cascadeTrackingEnabled())
            {
                shardDeletedNodeIds = notNullOrEmpty(nodeStatusToNodeIds.get(SolrApiNodeStatus.NON_SHARD_DELETED));
                shardUpdatedNodeIds = notNullOrEmpty(nodeStatusToNodeIds.get(SolrApiNodeStatus.NON_SHARD_UPDATED));
            }
            List<Long> unknownNodeIds = notNullOrEmpty(nodeStatusToNodeIds.get(SolrApiNodeStatus.UNKNOWN));
            List<Long> updatedNodeIds = notNullOrEmpty(nodeStatusToNodeIds.get(SolrApiNodeStatus.UPDATED));

            if (!deletedNodeIds.isEmpty() || !shardDeletedNodeIds.isEmpty() || !shardUpdatedNodeIds.isEmpty() || !unknownNodeIds.isEmpty())
            {
                // fix up any secondary paths
                List<NodeMetaData> nodeMetaDatas = new ArrayList<>();

                // For all deleted nodes, fake the node metadata
                for (Long deletedNodeId : deletedNodeIds)
                {
                    Node node = nodeIdsToNodes.get(deletedNodeId);
                    NodeMetaData nodeMetaData = createDeletedNodeMetaData(node);
                    nodeMetaDatas.add(nodeMetaData);
                }

                if (!unknownNodeIds.isEmpty())
                {
                    NodeMetaDataParameters nmdp = new NodeMetaDataParameters();
                    nmdp.setNodeIds(unknownNodeIds);
                    // When deleting nodes, no additional information is required
                    nmdp.setIncludeChildIds(false);
                    nmdp.setIncludeChildAssociations(false);
                    nmdp.setIncludeAspects(false);
                    nmdp.setIncludePaths(false);
                    nmdp.setIncludeParentAssociations(false);
                    nmdp.setMaxResults(Integer.MAX_VALUE);

                    Optional<Collection<NodeMetaData>> nodesMetaDataFromRepository = getNodesMetaDataFromRepository(nmdp);
                    if (nodesMetaDataFromRepository.isEmpty())
                    {
                        // Using exception for flow handling to jump to single node processing.
                        throw new Exception("Error loading node metadata from repository for bulk delete.");
                    }
                    nodeMetaDatas.addAll(nodesMetaDataFromRepository.get());
                }

                for (NodeMetaData nodeMetaData : nodeMetaDatas)
                {
                    Node node = nodeIdsToNodes.get(nodeMetaData.getId());
                    if (nodeMetaData.getTxnId() > node.getTxnId())
                    {
                        // the node has moved on to a later transaction
                        // it will be indexed later
                        continue;
                    }
                }

                LOGGER.debug("Deleting");
                DeleteUpdateCommand delDocCmd = new DeleteUpdateCommand(request);
                String query = this.cloud.getQuery(FIELD_DBID, OR, deletedNodeIds, shardDeletedNodeIds, shardUpdatedNodeIds, unknownNodeIds);
                delDocCmd.setQuery(query);
                processor.processDelete(delDocCmd);
            }

            if (!updatedNodeIds.isEmpty() || !unknownNodeIds.isEmpty() || !shardUpdatedNodeIds.isEmpty())
            {
                NodeMetaDataParameters nmdp = new NodeMetaDataParameters();
                List<Long> nodeIds = new LinkedList<>();
                nodeIds.addAll(updatedNodeIds);
                nodeIds.addAll(unknownNodeIds);
                nodeIds.addAll(shardUpdatedNodeIds);
                nmdp.setNodeIds(nodeIds);
                nmdp.setIncludeChildIds(false);
                nmdp.setIncludeChildAssociations(false);
                // Getting Ancestor information when getting a batch of nodes from repository,
                // may contain large information to be stored in memory for a long time.
                nmdp.setIncludePaths(getPathsInNodeBatches);
                
                // Fetches bulk metadata
                nmdp.setMaxResults(Integer.MAX_VALUE);
                Optional<Collection<NodeMetaData>> nodesMetaDataFromRepository = getNodesMetaDataFromRepository(nmdp);
                if (nodesMetaDataFromRepository.isEmpty())
                {
                    // Using exception for flow handling to jump to single node processing.
                    throw new Exception("Error loading node metadata from repository for bulk update.");
                }
                
                NEXT_NODE:
                for (NodeMetaData nodeMetaData : nodesMetaDataFromRepository.get())
                {
                    long start = System.nanoTime();

                    Node node = nodeIdsToNodes.get(nodeMetaData.getId());
                    if (nodeMetaData.getTxnId() > node.getTxnId())
                    {
                        // the node has moved on to a later transaction
                        // it will be indexed later
                        continue;
                    }

                        if (cascadeTrackingEnabled() && nodeIdsToNodes.get(nodeMetaData.getId()).getStatus() == SolrApiNodeStatus.NON_SHARD_UPDATED)
                        {
                            if (nodeMetaData.getProperties().get(ContentModel.PROP_CASCADE_TX) != null)
                            {
                                indexNonShardCascade(nodeMetaData);
                            }

                        continue;
                    }


                    AddUpdateCommand addDocCmd = new AddUpdateCommand(request);
                    addDocCmd.overwrite = overwrite;

                    // check index control
                    Map<QName, PropertyValue> properties = nodeMetaData.getProperties();
                    StringPropertyValue pValue = (StringPropertyValue) properties.get(ContentModel.PROP_IS_INDEXED);
                    if (pValue != null)
                    {
                        boolean isIndexed = Boolean.parseBoolean(pValue.getValue());
                        if (!isIndexed)
                        {
                            deleteNode(processor, request, node);
                            addDocCmd.solrDoc = basicDocument(nodeMetaData, DOC_TYPE_UNINDEXED_NODE, SolrInputDocument::new);
                            if (recordUnindexedNodes)
                            {
                                processor.processAdd(addDocCmd);
                            }

                            this.trackerStats.addNodeTime(System.nanoTime() - start);
                            continue NEXT_NODE;
                        }
                    }

                    // Make sure any unindexed or error doc is removed.
                    deleteErrorNode(processor, request, node);

                    addDocCmd.solrDoc =
                            populateWithMetadata(basicDocument(nodeMetaData, DOC_TYPE_NODE, PartialSolrInputDocument::new),
                                    nodeMetaData, nmdp);
                    processor.processAdd(addDocCmd);

                    this.trackerStats.addNodeTime(System.nanoTime() - start);
                }
            }
        }
        catch (Exception e)
        {
            LOGGER.error(" Bulk indexing failed, do one node at a time. See the stacktrace below for further details.", e);
            for (Node node : nodes)
            {
                this.indexNode(node, true);
            }
        }
        finally
        {
            if (processor != null)
            {
                processor.finish();
            }
        }
    }

    private PartialSolrInputDocument populateWithMetadata(PartialSolrInputDocument document, NodeMetaData metadata, NodeMetaDataParameters nmdp)
    {
        populateFields(metadata, document, nmdp);

        LOGGER.debug("Document size (fields) after getting fields from node {} metadata: {}", metadata.getId(), document.size());

        populateProperties(
                metadata.getProperties(),
                isContentIndexedForNode(metadata.getProperties()),
                document,
                contentIndexingHasBeenEnabledOnThisInstance);

        keepContentFields(document);

        LOGGER.debug("Document size (fields) after getting properties from node {} metadata: {}", metadata.getId(), document.size());

        return document;
    }

    private void populateFields(NodeMetaData metadata, SolrInputDocument doc, NodeMetaDataParameters nmdp)
    {
        doc.setField(FIELD_TYPE, metadata.getType().toString());
        if (nmdp.isIncludeAspects())
        {
            doc.removeField(FIELD_ASPECT);
            notNullOrEmpty(metadata.getAspects())
                    .stream()
                    .filter(Objects::nonNull)
                    .forEach(aspect -> {
                        doc.addField(FIELD_ASPECT, aspect.toString());
                        if(aspect.equals(ContentModel.ASPECT_GEOGRAPHIC))
                        {
                            Optional<Double> latitude =
                                    ofNullable(metadata.getProperties().get(ContentModel.PROP_LATITUDE))
                                            .map(StringPropertyValue.class::cast)
                                            .map(StringPropertyValue::getValue)
                                            .map(Utils::doubleOrNull)
                                            .filter(value -> -90d <= value && value <= 90d);
    
                            Optional<Double> longitude =
                                    ofNullable(metadata.getProperties().get(ContentModel.PROP_LONGITUDE))
                                            .map(StringPropertyValue.class::cast)
                                            .map(StringPropertyValue::getValue)
                                            .map(Utils::doubleOrNull)
                                            .filter(value -> -180d <= value && value <= 180d);
    
                            if (latitude.isPresent() && longitude.isPresent())
                            {
                                doc.setField(FIELD_GEO, latitude.get() + ", " + longitude.get());
                            }
                            else
                            {
                                LOGGER.warning("Skipping missing geo data on node {}", metadata.getId());
                            }
                        }
                    });
        }

        doc.setField(FIELD_ISNODE, "T");
        doc.setField(FIELD_TENANT, AlfrescoSolrDataModel.getTenantId(metadata.getTenantDomain()));

        if (cascadeTrackingEnabled())
        {
            // As metadata is used like final but the lambdas above, we need a new variable here
            NodeMetaData extendedMetadata = metadata;
            // Ancestor information was not recovered for node batches, so we need to update
            // the node with that information before updating the SOLR Document
            if (!getPathsInNodeBatches)
            {
                extendedMetadata = getNodeMetaDataWithPathInfo(metadata.getId());
            }
            updatePathRelatedFields(extendedMetadata, doc);
            updateNamePathRelatedFields(extendedMetadata, doc);
            updateAncestorRelatedFields(extendedMetadata, doc);
            doc.setField(FIELD_PARENT_ASSOC_CRC, extendedMetadata.getParentAssocsCrc());
        }

        ofNullable(metadata.getOwner()).ifPresent(owner -> doc.setField(FIELD_OWNER, owner));

        StringBuilder qNameBuffer = new StringBuilder();
        StringBuilder assocTypeQNameBuffer = new StringBuilder();

        if (nmdp.isIncludeParentAssociations())
        {
            doc.removeField(FIELD_PARENT);
            notNullOrEmpty(metadata.getParentAssocs())
                    .forEach(childAssocRef -> {
                        if (qNameBuffer.length() > 0)
                        {
                            qNameBuffer.append(";/");
                            assocTypeQNameBuffer.append(";/");
                        }
                        qNameBuffer.append(ISO9075.getXPathName(childAssocRef.getQName()));
                        assocTypeQNameBuffer.append(ISO9075.getXPathName(childAssocRef.getTypeQName()));
                        doc.addField(FIELD_PARENT, childAssocRef.getParentRef().toString());
    
                        if (childAssocRef.isPrimary())
                        {
                            if(doc.getField(FIELD_PRIMARYPARENT) == null)
                            {
                                doc.setField(FIELD_PRIMARYPARENT, childAssocRef.getParentRef().toString());
                                doc.setField(FIELD_PRIMARYASSOCTYPEQNAME, ISO9075.getXPathName(childAssocRef.getTypeQName()));
                                doc.setField(FIELD_PRIMARYASSOCQNAME, ISO9075.getXPathName(childAssocRef.getQName()));
                            }
                            else
                            {
                                LOGGER.warning("Duplicate primary parent for node id {}", metadata.getId());
                            }
                        }
                    });
        }

        ofNullable(metadata.getParentAssocs()).ifPresent(parents -> {
            doc.addField(FIELD_ASSOCTYPEQNAME, assocTypeQNameBuffer.toString());
            doc.addField(FIELD_QNAME, qNameBuffer.toString());
        });
    }
    
    /**
     * Gets full metadata information for a given nodeId, including Paths information.
     * Paths information can be huge in some scenarios, so it's recommended to use 
     * this method always, as this gets Paths information for a single node. 
     * @param nodeId Id for the node to get information from repository
     * @return Full metadata information for the node
     */
    private NodeMetaData getNodeMetaDataWithPathInfo(long nodeId)
    {
        NodeMetaDataParameters nmdp = new NodeMetaDataParameters();
        nmdp.setFromNodeId(nodeId);
        nmdp.setToNodeId(nodeId);
        nmdp.setIncludePaths(true);
        nmdp.setMaxResults(1);
        return getNodesMetaDataFromRepository(nmdp).get().iterator().next();
    }
    
    private void updateAncestorRelatedFields(NodeMetaData nodeMetaData, SolrInputDocument doc)
    {
        doc.removeField(FIELD_ANCESTOR);
        notNullOrEmpty(nodeMetaData.getAncestors())
                .stream()
                .map(Object::toString)
                .forEach(ancestor -> doc.addField(FIELD_ANCESTOR, ancestor));
    }

    private void updateNamePathRelatedFields(NodeMetaData nodeMetaData, SolrInputDocument doc)
    {
        clearFields(doc, FIELD_NPATH, FIELD_PNAME);

        for(List<String> namePath : nodeMetaData.getNamePaths())
        {
            StringBuilder builder = new StringBuilder();
            int i = 0;
            for(String element : namePath)
            {
                builder.append('/').append(element);
                doc.addField(FIELD_NPATH, "" + i++ + builder);
            }

            if(builder.length() > 0)
            {
                doc.addField(FIELD_NPATH, "F" + builder);
            }

            builder = new StringBuilder();
            for(int j = 0;  j < namePath.size() - 1; j++)
            {
                String element = namePath.get(namePath.size() - 2 - j);
                builder.insert(0, element);
                builder.insert(0, '/');
                doc.addField(FIELD_PNAME, "" + j + builder);
            }

            if(builder.length() > 0)
            {
                doc.addField(FIELD_PNAME, "F" + builder);
            }
        }
    }

    void mltextProperty(QName propertyQName, MLTextPropertyValue value, final BiConsumer<String, Object> valueHolder)
    {
        AlfrescoSolrDataModel dataModel = AlfrescoSolrDataModel.getInstance();
        List<FieldInstance> fields = dataModel.getIndexedFieldNamesForProperty(propertyQName).getFields();

        String storedFieldName = dataModel.getStoredMLTextField(propertyQName);

        valueHolder.accept(storedFieldName, getLocalisedValues(value));
        fields.stream()
            .filter(FieldInstance::isSort)
            .forEach(field -> addMLTextProperty(valueHolder, field, value));
    }

    void stringProperty(QName propertyQName, StringPropertyValue value, PropertyValue locale, final BiConsumer<String, Object> valueHolder)
    {
        AlfrescoSolrDataModel dataModel = AlfrescoSolrDataModel.getInstance();
        PropertyDefinition definition = dataModel.getPropertyDefinition(propertyQName);

        if (dataModel.isTextField(definition))
        {
            String storedFieldName = dataModel.getStoredTextField(propertyQName);
            valueHolder.accept(storedFieldName, getLocalisedValue(value, locale));

            // Add identifiers for single valued (sd) and multi-valued (md) identifier fields
            dataModel.getIndexedFieldNamesForProperty(propertyQName).getFields()
                    .stream()
                    .filter(field -> field.getField().startsWith("text@sd___@") || field.getField().startsWith("text@md___@"))
                    .forEach(field -> addStringProperty(valueHolder, field, value, locale));
        } 
        else
        {
            dataModel.getIndexedFieldNamesForProperty(propertyQName).getFields()
                    .forEach(field -> {
                        if (canBeDestructured(definition, field.getField()))
                        {
                            setUnitOfTimeFields(valueHolder, field.getField(), value.getValue(), definition.getDataType());
                        }
                        addStringProperty(valueHolder, field, value, locale);
                    });
        }
    }

    void populateProperties(
            Map<QName, PropertyValue> properties,
            boolean contentIndexingHasBeenRequestedForThisNode,
            SolrInputDocument document,
            boolean contentIndexingHasBeenEnabledOnThisInstance)
    {
        boolean contentIndexingIsEnabled =
                contentIndexingHasBeenEnabledOnThisInstance
                        && contentIndexingHasBeenRequestedForThisNode;

        if (!contentIndexingIsEnabled)
        {
            markAsContentInSynch(document);
        }

        final BiConsumer<String, Object> setValue = document::setField;
        final BiConsumer<String, Object> addValue = document::addField;
        final BiConsumer<String, Object> collectName = (name, value) -> addFieldIfNotSet(document, name);
        final BiConsumer<String, Object> setAndCollect = setValue.andThen(collectName);
        final BiConsumer<String, Object> addAndCollect = addValue.andThen(collectName);

        for (Entry<QName, PropertyValue> property : properties.entrySet())
        {
            
            QName propertyQName =  property.getKey();
            PropertyDefinition propertyDefinition = dataModel.getPropertyDefinition(propertyQName);          
            
            // Skip adding Alfresco Fields declared as indexed="false" to SOLR Schema
            if (propertyDefinition != null && propertyDefinition.isIndexed())
            {
            
                document.addField(FIELD_PROPERTIES, propertyQName.toString());
                document.addField(FIELD_PROPERTIES, propertyQName.getPrefixString());
    
                PropertyValue value = property.getValue();
                if(value != null)
                {
                    if (value instanceof StringPropertyValue)
                    {
                        stringProperty(propertyQName, (StringPropertyValue) value, properties.get(ContentModel.PROP_LOCALE), setAndCollect);
                    }
                    else if (value instanceof MLTextPropertyValue)
                    {
                        mltextProperty(propertyQName,(MLTextPropertyValue) value, setAndCollect);
                    }
                    else if (value instanceof ContentPropertyValue)
                    {
                        addContentProperty(setAndCollect, document, propertyQName, (ContentPropertyValue) value, contentIndexingIsEnabled);
                    }
                    else if (value instanceof MultiPropertyValue)
                    {
                        MultiPropertyValue typedValue = (MultiPropertyValue) value;
                        AlfrescoSolrDataModel dataModel = AlfrescoSolrDataModel.getInstance();
                        clearFields(
                                document,
                                dataModel.getIndexedFieldNamesForProperty(propertyQName).getFields()
                                    .stream()
                                    .map(FieldInstance::getField)
                                    .collect(Collectors.toList()));
    
                        for (PropertyValue singleValue : typedValue.getValues())
                        {
                            if (singleValue instanceof StringPropertyValue)
                            {
                                stringProperty(propertyQName, (StringPropertyValue) singleValue, properties.get(ContentModel.PROP_LOCALE), addAndCollect);
                            }
                            else if (singleValue instanceof MLTextPropertyValue)
                            {
                                mltextProperty(propertyQName,(MLTextPropertyValue) singleValue, addAndCollect);
                            }
                            else if (singleValue instanceof ContentPropertyValue)
                            {
                                addContentProperty(addAndCollect, document, propertyQName, (ContentPropertyValue) singleValue, contentIndexingIsEnabled);
                            }
                        }
                    }
                }
                else
                {
                    document.addField(FIELD_NULLPROPERTIES, propertyQName.toString());
                }
                
            }
            else
            {
                LOGGER.debug("Field '" + propertyQName + "' has not been indexed "
                        + (propertyDefinition != null ? "as property definition is not found."
                                : "as it has been declared as not indexable in the Content Model."));
            }
        }
    }

    private void deleteErrorNode(UpdateRequestProcessor processor, SolrQueryRequest request, Node node) throws IOException
    {
        String errorDocId = PREFIX_ERROR + node.getId();
        if (getDocListSize(FIELD_SOLR4_ID + ":" + errorDocId) > 0)
        {
            DeleteUpdateCommand delErrorDocCmd = new DeleteUpdateCommand(request);
            delErrorDocCmd.setId(errorDocId);
            processor.processDelete(delErrorDocCmd);
        }
    }

    private void deleteNode(UpdateRequestProcessor processor, SolrQueryRequest request, long dbid) throws IOException
    {
        if (getDocListSize(FIELD_DBID + ":" + dbid) > 0)
        {
            DeleteUpdateCommand delDocCmd = new DeleteUpdateCommand(request);
            delDocCmd.setQuery(FIELD_DBID + ":" + dbid);
            processor.processDelete(delDocCmd);
        }
    }

    private void deleteNode(UpdateRequestProcessor processor, SolrQueryRequest request, Node node) throws IOException
    {
        LOGGER.debug("Node {} is being deleted", node.getId());

        deleteErrorNode(processor, request, node);
        deleteNode(processor, request, node.getId());

        LOGGER.debug("Node {} deletion correctly sent", node.getId());
    }

    private boolean isContentIndexedForNode(Map<QName, PropertyValue> properties)
    {
        return ofNullable(properties)
                .map(map -> map.get(ContentModel.PROP_IS_CONTENT_INDEXED))
                .map(StringPropertyValue.class::cast)
                .map(StringPropertyValue::getValue)
                .map(Boolean::parseBoolean)
                .orElse(true);
    }

    private void categorizeNodes(
            List<Node> nodes,
            Map<Long, Node> nodeIdsToNodes,
            EnumMap<SolrApiNodeStatus, List<Long>> nodeStatusToNodeIds)
    {
        for (Node node : nodes)
        {
            nodeIdsToNodes.put(node.getId(), node);

            List<Long> nodeIds = nodeStatusToNodeIds.computeIfAbsent(node.getStatus(), k -> new LinkedList<>());
            nodeIds.add(node.getId());
        }
    }

    private void addContentPropertyMetadata(
            SolrInputDocument doc,
            QName propertyQName,
            AlfrescoSolrDataModel.SpecializedFieldType type,
            GetTextContentResponse textContentResponse)
    {
        IndexedField indexedField = dataModel.getIndexedFieldForSpecializedPropertyMetadata(propertyQName, type);
        for (FieldInstance fieldInstance : indexedField.getFields())
        {
            switch(type)
            {
            case TRANSFORMATION_EXCEPTION:
                doc.setField(fieldInstance.getField(), textContentResponse.getTransformException());
                break;
            case TRANSFORMATION_STATUS:
                doc.setField(fieldInstance.getField(), textContentResponse.getStatus().name());
                break;
            case TRANSFORMATION_TIME:
                doc.setField(fieldInstance.getField(), textContentResponse.getTransformDuration());
                break;
                // Skips the ones that require the ContentPropertyValue
                default:
                break;
            }
        }
    }

    private void addContentPropertyMetadata(
            BiConsumer<String, Object> consumer,
            QName propertyQName,
            ContentPropertyValue contentPropertyValue,
            AlfrescoSolrDataModel.SpecializedFieldType type)
    {
        IndexedField indexedField =
                AlfrescoSolrDataModel.getInstance().getIndexedFieldForSpecializedPropertyMetadata(propertyQName, type);
        for (FieldInstance fieldInstance : indexedField.getFields())
        {
            switch(type)
            {
            case CONTENT_DOCID:
                consumer.accept(fieldInstance.getField(), contentPropertyValue.getId());
                break;
            case CONTENT_ENCODING:
                consumer.accept(fieldInstance.getField(), contentPropertyValue.getEncoding());
                break;
            case CONTENT_LOCALE:
                consumer.accept(fieldInstance.getField(), contentPropertyValue.getLocale().toString());
                break;
            case CONTENT_MIMETYPE:
                consumer.accept(fieldInstance.getField(), contentPropertyValue.getMimetype());
                break;
            case CONTENT_SIZE:
                consumer.accept(fieldInstance.getField(), contentPropertyValue.getLength());
                break;
                // Skips the ones that require the text content response
                default:
                break;
            }
        }
    }

    /**
     * Sets the two fields used for marking a document as ignored by the ContentTracker.
     * In other words, once a {@link SolrInputDocument} passes through this method, the ContentTracker will ignore it.
     *
     * @see #insertContentUpdateMarker(SolrInputDocument, ContentPropertyValue)
     */
    private void markAsContentInSynch(SolrInputDocument document)
    {
        markAsContentInSynch(document, (ContentPropertyValue)null);
    }

    /**
     * Sets the two fields used for marking a document as ignored by the ContentTracker.
     * In other words, once a {@link SolrInputDocument} passes through this method, the ContentTracker will ignore it.
     *
     * @see #insertContentUpdateMarker(SolrInputDocument, ContentPropertyValue)
     */
    private void markAsContentInSynch(SolrInputDocument document, ContentPropertyValue value)
    {
        ofNullable(value)
                .map(ContentPropertyValue::getId)
                .ifPresentOrElse(
                    id -> markAsContentInSynch(document, id),
                    () -> markAsContentInSynch(document, CONTENT_UPDATED_MARKER));
    }

    /**
     * Sets the two fields used for marking a document as ignored by the ContentTracker.
     * In other words, once a {@link SolrInputDocument} passes through this method, the ContentTracker will ignore it.
     *
     * @see #insertContentUpdateMarker(SolrInputDocument, ContentPropertyValue)
     */
    private void markAsContentInSynch(SolrInputDocument document, Long id)
    {
        long contentVersionId = ofNullable(id).orElse(CONTENT_UPDATED_MARKER);

        document.setField(LATEST_APPLIED_CONTENT_VERSION_ID, contentVersionId);
        document.setField(LAST_INCOMING_CONTENT_VERSION_ID, contentVersionId);
    }

    /**
     * Inserts a special atomic update command for updating the marker field used for detecting outdated content.
     * In order to have a better understanding of this method: before removing the content store, the information
     * about the document status was captured using the FTSSTATUS and DOCID (NB: DOCID is not the lucene DOCID; instead
     * it meant to be a content version identifier).
     *
     * FTSSTATUS = New
     *
     * The very first time a document/node arrived, and content indexing was required, FTSSTATUS was set to New.
     * Solr doesn't make any difference between inserts and updates so how was it possible to assign the "New" value to
     * such field? Instead of querying Solr in order to say "Does this doc already exist there?" this class used the
     * content store for doing the same thing (on the filesystem). So in other words,
     *
     * - Node with id X arrives
     * - Do we have an entry in the local content store for the document X?
     * - If we didn't have that entry then FTSSTATUS was set to New
     *
     * FTSSTATUS = DIRTY
     *
     * When a node, which requires content, arrived here for being indexed, the "Dirty" status was set if
     *
     * - the node was not "New" (see above)
     * - the DOCID value was different between the incoming node and the existing field value in Solr
     *
     * Again, at index time, we don't have the document which has been indexed in Solr: we create our document
     * which will replace that once it is sent to Solr. But we cannot say "mark this field with this value only if
     * something is different between the local document and the document which is in Solr".
     *
     * The content store allowed to workaround the problem: the code was deserializing the entry corresponding to the
     * incoming node (which theoretically corresponded to the document indexed in Solr) so both versions were available
     * for making decisions
     *
     * ------------------
     *
     * The current implementation, which removed at all the content store and uses Atomic Updates, doesn't have any idea
     * about
     *
     * - the "INSERT" or "UPDATE" nature of the indexing operation that is going to be executed
     * - the values of fields of a given document indexed in Solr
     *
     * In order to indicate to the ContentTracker which documents will require the content update, we added two
     * additional fields in the schema:
     *
     * <ul>
     *     <li>
     *          LATEST_APPLIED_CONTENT_VERSION_ID: as the name suggests, this is the latest DOCID applied to this document (again, not the lucene docid)
     *     </li>
     *     <li>
     *          LAST_INCOMING_CONTENT_VERSION_ID: this call will set the field to "-10" as the content is outdated. The content tracker will then set it
     *          to have same value as LATEST_APPLIED_CONTENT_VERSION_ID.
     *     </li>
     * </ul>
     *
     * As a side note, please keep in mind the FTSSTATUS field has been removed.
     */
    private void insertContentUpdateMarker(SolrInputDocument document, ContentPropertyValue value)
    {
        ofNullable(value)
                .map(ContentPropertyValue::getId)
                .ifPresent(id -> document.setField(LATEST_APPLIED_CONTENT_VERSION_ID, id));
        document.setField(LAST_INCOMING_CONTENT_VERSION_ID, CONTENT_OUTDATED_MARKER);
    }

    private void addContentProperty(
            BiConsumer<String, Object> consumer,
            SolrInputDocument document,
            QName propertyName,
            ContentPropertyValue propertyValue,
            boolean contentIndexingEnabled)
    {
        addContentPropertyMetadata(consumer, propertyName, propertyValue, AlfrescoSolrDataModel.SpecializedFieldType.CONTENT_DOCID);
        addContentPropertyMetadata(consumer, propertyName, propertyValue, AlfrescoSolrDataModel.SpecializedFieldType.CONTENT_SIZE);
        addContentPropertyMetadata(consumer, propertyName, propertyValue, AlfrescoSolrDataModel.SpecializedFieldType.CONTENT_LOCALE);
        addContentPropertyMetadata(consumer, propertyName, propertyValue, AlfrescoSolrDataModel.SpecializedFieldType.CONTENT_MIMETYPE);
        addContentPropertyMetadata(consumer, propertyName, propertyValue, AlfrescoSolrDataModel.SpecializedFieldType.CONTENT_ENCODING);

        if (contentIndexingEnabled)
        {
            insertContentUpdateMarker(document, propertyValue);
        }
    }

    private void addContentToDoc(TenantDbId docRef, SolrInputDocument doc, long dbId) throws AuthenticationException, IOException
    {
        String locale = (String) docRef.optionalBag.get(CONTENT_LOCALE_FIELD);
        String qNamePart = CONTENT_LOCALE_FIELD.substring(AlfrescoSolrDataModel.CONTENT_S_LOCALE_PREFIX.length());
        QName propertyQName = QName.createQName(qNamePart);
        addContentPropertyToDocUsingAlfrescoRepository(doc, propertyQName, dbId, locale);
    }



    /**
     * Extracts the text content from the given API response.
     *
     * @param response the API (GetTextContent) response.
     * @return the text content from the given API response.
     * @throws IOException in case of I/O failure.
     */
    private String textContentFrom(GetTextContentResponse response) throws IOException
    {
        try (final InputStream ris = ofNullable(response.getContentEncoding())
                .map(c -> c.equals("gzip")).orElse(false)?
                new GZIPInputStream(response.getContent()) : response.getContent())
        {
            if (ris != null)
            {
                byte[] bytes = FileCopyUtils.copyToByteArray(new BoundedInputStream(ris, contentStreamLimit));
                return new String(bytes, StandardCharsets.UTF_8);
            }
            return "";
        }
        finally
        {
            response.release();
        }
    }

    private void addContentPropertyToDocUsingAlfrescoRepository(
            SolrInputDocument doc,
            QName propertyQName,
            long dbId,
            String locale) throws AuthenticationException, IOException
    {
        long start = System.nanoTime();

        // Expensive call to be done with ContentTracker
        try (GetTextContentResponse response = repositoryClient.getTextContent(dbId, propertyQName, null)) {
            addContentPropertyMetadata(doc, propertyQName, AlfrescoSolrDataModel.SpecializedFieldType.TRANSFORMATION_STATUS, response);
            addContentPropertyMetadata(doc, propertyQName, AlfrescoSolrDataModel.SpecializedFieldType.TRANSFORMATION_EXCEPTION, response);
            addContentPropertyMetadata(doc, propertyQName, AlfrescoSolrDataModel.SpecializedFieldType.TRANSFORMATION_TIME, response);

            final String textContent = textContentFrom(response);

            if (fingerprintHasBeenEnabledOnThisInstance && !textContent.isBlank()) {
                Analyzer analyzer = core.getLatestSchema().getFieldType("min_hash").getIndexAnalyzer();
                TokenStream ts = analyzer.tokenStream("dummy_field", textContent);
                CharTermAttribute termAttribute = ts.getAttribute(CharTermAttribute.class);
                ts.reset();
                doc.removeField(FINGERPRINT_FIELD);
                while (ts.incrementToken())
                {
                    StringBuilder tokenBuff = new StringBuilder();
                    char[] buff = termAttribute.buffer();

                    for (int i = 0; i < termAttribute.length(); i++) {
                        tokenBuff.append(Integer.toHexString(buff[i]));
                    }
                    doc.addField(FINGERPRINT_FIELD, tokenBuff.toString());

                }
                ts.end();
                ts.close();
            }

            this.getTrackerStats().addDocTransformationTime(System.nanoTime() - start);

            String storedField = dataModel.getStoredContentField(propertyQName);
            doc.setField(storedField, "\u0000" + languageFrom(locale) + "\u0000" + textContent);

            dataModel.getIndexedFieldNamesForProperty(propertyQName)
                    .getFields()
                    .forEach(field -> addFieldIfNotSet(doc, field.getField()));
        }
    }

    private void keepContentFields(PartialSolrInputDocument doc)
    {
        String qNamePart = CONTENT_LOCALE_FIELD.substring(AlfrescoSolrDataModel.CONTENT_S_LOCALE_PREFIX.length());
        QName propertyQName = QName.createQName(qNamePart);

        dataModel.getIndexedFieldForSpecializedPropertyMetadata(propertyQName, AlfrescoSolrDataModel.SpecializedFieldType.TRANSFORMATION_STATUS)
                .getFields()
                .stream()
                .forEach(field -> doc.keepField(field.getField()));

        dataModel.getIndexedFieldForSpecializedPropertyMetadata(propertyQName, AlfrescoSolrDataModel.SpecializedFieldType.TRANSFORMATION_EXCEPTION)
                .getFields()
                .stream()
                .forEach(field -> doc.keepField(field.getField()));

        dataModel.getIndexedFieldForSpecializedPropertyMetadata(propertyQName, AlfrescoSolrDataModel.SpecializedFieldType.TRANSFORMATION_TIME)
                .getFields()
                .stream()
                .forEach(field -> doc.keepField(field.getField()));

        doc.keepField(FINGERPRINT_FIELD);
        doc.keepField(dataModel.getStoredContentField(propertyQName));
    }

    private String languageFrom(String locale)
    {
        int indexOfSeparator = locale.indexOf("_");
        return indexOfSeparator == -1 ? locale : locale.substring(0, indexOfSeparator);
    }

    private List<String> getLocalisedValues(MLTextPropertyValue mlTextPropertyValue)
    {
        if (mlTextPropertyValue == null)
        {
            return Collections.emptyList();
        }

        List<String> values = new ArrayList<>();
        for (Locale locale : mlTextPropertyValue.getLocales())
        {
            final String propValue = mlTextPropertyValue.getValue(locale);
            if((locale == null) || propValue == null)
            {
                continue;
            }

            values.add("\u0000" + locale.getLanguage() + "\u0000" + propValue);
        }
        return values;
    }

    private void addMLTextProperty(BiConsumer<String, Object> consumer, FieldInstance field, MLTextPropertyValue mlTextPropertyValue)
    {
        if (mlTextPropertyValue == null)
        {
            return;
        }

        if(field.isLocalised())
        {
            StringBuilder sort = new StringBuilder();
            for (Locale locale : mlTextPropertyValue.getLocales())
            {
                final String propValue = mlTextPropertyValue.getValue(locale);
                if((locale == null) || propValue == null)
                {
                    continue;
                }

                StringBuilder builder = new StringBuilder(propValue.length() + 16);
                builder.append("\u0000").append(locale.toString()).append("\u0000").append(propValue);

                if(!field.isSort())
                {
                    consumer.accept(field.getField(), builder.toString());
                }

                if (sort.length() > 0)
                {
                    sort.append("\u0000");
                }
                sort.append(builder.toString());
            }

            if(field.isSort())
            {
                consumer.accept(field.getField(), sort.toString());
            }
        }
        else
        {
            List<String> localisedValues =
                    notNullOrEmpty(mlTextPropertyValue.getLocales())
                        .stream()
                        .filter(Objects::nonNull)
                        .map(mlTextPropertyValue::getValue)
                        .collect(Collectors.toList());

            if (!localisedValues.isEmpty())
            {
                consumer.accept(field.getField(), localisedValues);
            }
        }
    }

    @Override
    public void updateTransaction(Transaction txn) throws IOException
    {
        canUpdate();
        UpdateRequestProcessor processor = null;
        try (SolrQueryRequest request = newSolrQueryRequest())
        {
            processor = this.core.getUpdateProcessingChain(null).createProcessor(request, newSolrQueryResponse());

            AddUpdateCommand cmd = new AddUpdateCommand(request);
            cmd.overwrite = true;
            SolrInputDocument input = new SolrInputDocument();
            input.addField(FIELD_SOLR4_ID, AlfrescoSolrDataModel.getTransactionDocumentId(txn.getId()));
            input.addField(FIELD_VERSION, 1);
            input.addField(FIELD_TXID, txn.getId());
            input.addField(FIELD_INTXID, txn.getId());
            input.addField(FIELD_TXCOMMITTIME, txn.getCommitTimeMs());
            input.addField(FIELD_DOC_TYPE, DOC_TYPE_TX);
            if (cascadeTrackingEnabled())
            {
                input.addField(FIELD_CASCADE_FLAG, 0);
            }
            cmd.solrDoc = input;
            processor.processAdd(cmd);
        }
        finally
        {
            if (processor != null)
            {
                processor.finish();
            }
        }
    }

    @Override
    public void indexTransaction(Transaction info, boolean overwrite) throws IOException
    {
        canUpdate();
        UpdateRequestProcessor processor = null;
        try (SolrQueryRequest request = newSolrQueryRequest())
        {
            processor = this.core.getUpdateProcessingChain(null).createProcessor(request, newSolrQueryResponse());

            AddUpdateCommand cmd = new AddUpdateCommand(request);
            cmd.overwrite = overwrite;
            SolrInputDocument input = new SolrInputDocument();
            input.addField(FIELD_SOLR4_ID, AlfrescoSolrDataModel.getTransactionDocumentId(info.getId()));
            input.addField(FIELD_VERSION, 0);
            input.addField(FIELD_TXID, info.getId());
            input.addField(FIELD_INTXID, info.getId());
            input.addField(FIELD_TXCOMMITTIME, info.getCommitTimeMs());
            input.addField(FIELD_DOC_TYPE, DOC_TYPE_TX);

            /*
                For backwards compat reasons adding 3 new stored fields. 2 of these fields are duplicate data but there are needed so that
                we can properly update the transaction record for ACE-4284.
            */
            //This fields will be used to update the transaction record
            //They will only be on the record until the cascading updates for this transaction are processed
            input.addField(FIELD_S_TXID, info.getId());
            input.addField(FIELD_S_TXCOMMITTIME, info.getCommitTimeMs());

            if (cascadeTrackingEnabled())
            {
                //Set the cascade flag to 1. This means cascading updates have not been done yet.
                input.addField(FIELD_CASCADE_FLAG, 1);
            }

            cmd.solrDoc = input;
            processor.processAdd(cmd);

            putTransactionState(processor, request, info);
        }
        finally
        {
            if (processor != null)
            {
                processor.finish();
            }
        }
    }

    /**
     * Index information of a node that does not belong to the current shard.
     * These information are necessary for cascade tracker to work properly.
     * The information stored are:
     *      nodeDocumentId, cascadeTx
     */
    private void indexNonShardCascade(NodeMetaData nodeMetaData) throws IOException
    {

        UpdateRequestProcessor processor = null;
        try (SolrQueryRequest request = newSolrQueryRequest())
        {
            processor = this.core.getUpdateProcessingChain(null).createProcessor(request, newSolrQueryResponse());
            StringPropertyValue stringPropertyValue = (StringPropertyValue) nodeMetaData.getProperties().get(ContentModel.PROP_CASCADE_TX);
            List<FieldInstance> fieldInstances = AlfrescoSolrDataModel.getInstance().getIndexedFieldNamesForProperty(ContentModel.PROP_CASCADE_TX).getFields();
            FieldInstance fieldInstance = fieldInstances.get(0);
            AddUpdateCommand cmd = new AddUpdateCommand(request);
            SolrInputDocument input = new SolrInputDocument();
            input.addField(FIELD_SOLR4_ID, AlfrescoSolrDataModel.getNodeDocumentId(nodeMetaData.getTenantDomain(), nodeMetaData.getId()));
            input.addField(FIELD_VERSION, 0);
            input.addField(fieldInstance.getField(), stringPropertyValue.getValue());
            cmd.solrDoc = input;
            processor.processAdd(cmd);

        }
        finally
        {
            if (processor != null)
            {
                processor.finish();
            }
        }
    }

    @Override
    public Transaction getMaxTransactionIdAndCommitTimeInIndex()
    {
        try (SolrQueryRequest request = newSolrQueryRequest())
        {
            SolrDocument txState = getState(core, request, "TRACKER!STATE!TX");
            Transaction maxTransaction;
            if (txState != null)
            {
                long id = this.getFieldValueLong(txState, FIELD_S_TXID);
                long commitTime = this.getFieldValueLong(txState, FIELD_S_TXCOMMITTIME);
                maxTransaction = new Transaction();
                maxTransaction.setId(id);
                maxTransaction.setCommitTimeMs(commitTime);
            } else
                {
                maxTransaction = new Transaction();
            }
            return maxTransaction;
        }
    }

    @Override
    public boolean isInIndex(String id)
    {
        Boolean found = isIdIndexCache.get(id);
        if(found != null)
        {
            return found;
        }

        boolean isInIndex = isInIndexImpl(id);
        if(isInIndex)
        {
            isIdIndexCache.put(id, Boolean.TRUE);
        }
        return isInIndex;
    }

    @Override
    public List<Transaction> getCascades(int num) throws IOException
    {
        RefCounted<SolrIndexSearcher> refCounted = null;
        try
        {
            refCounted = this.core.getSearcher();
            SolrIndexSearcher searcher = refCounted.get();

            Collector collector;

            TopFieldCollector topFieldCollector = TopFieldCollector.create(new Sort(new SortField(FIELD_TXID, SortField.Type.LONG)),
                                                                            num,
                                                                            null,
                                                                            false,
                                                                            false,
                                                                            false);

            collector = topFieldCollector;

            LegacyNumericRangeQuery q = LegacyNumericRangeQuery.newIntRange(FIELD_CASCADE_FLAG, 1, 1, true, true);
            DelegatingCollector delegatingCollector = new TxnCacheFilter(cleanCascadeCache);

            delegatingCollector.setLastDelegate(collector);
            collector = delegatingCollector;

            searcher.search(q, collector);
            ScoreDoc[] scoreDocs = topFieldCollector.topDocs().scoreDocs;

            Set<String> fields = new HashSet<>();
            fields.add(FIELD_S_TXID);
            fields.add(FIELD_S_TXCOMMITTIME);

            List<Transaction> transactions = new ArrayList<>(scoreDocs.length);

            for(ScoreDoc scoreDoc : scoreDocs)
            {
                Transaction transaction = new Transaction();
                Document doc = searcher.doc(scoreDoc.doc, fields);

                IndexableField txID = doc.getField(FIELD_S_TXID);
                long txnID = txID.numericValue().longValue();
                cleanCascadeCache.put(txnID, null);
                transaction.setId(txnID);

                IndexableField txnCommitTime = doc.getField(FIELD_S_TXCOMMITTIME);
                transaction.setCommitTimeMs(txnCommitTime.numericValue().longValue());

                transactions.add(transaction);
            }

            return transactions;
        }
        finally
        {
            ofNullable(refCounted).ifPresent(RefCounted::decref);
        }
    }

    @Override
    public boolean txnInIndex(long txnId, boolean populateCache) throws IOException
    {
        return isInIndex(txnId, txnIdCache, FIELD_TXID, populateCache, core);
    }

    @Override
    public boolean aclChangeSetInIndex(long changeSetId, boolean populateCache) throws IOException
    {
        return isInIndex(changeSetId, aclChangeSetCache, FIELD_ACLTXID, populateCache, core);
    }

    @Override
    public void clearProcessedTransactions()
    {
        this.txnIdCache.clear();
    }

    @Override
    public void clearProcessedAclChangeSets()
    {
        this.aclChangeSetCache.clear();
    }

    @Override
    public boolean putModel(M2Model model)
    {
        return this.dataModel.putModel(model);
    }

    @Override
    public void rollback() throws IOException
    {
        commitAndRollbackLock.writeLock().lock();
        try
        {
            activeTrackerThreadsLock.writeLock().lock();
            try
            {
                activeTrackerThreads.clear();

                UpdateRequestProcessor processor = null;
                try (SolrQueryRequest request = newSolrQueryRequest())
                {
                    processor = this.core.getUpdateProcessingChain(null).createProcessor(request, newSolrQueryResponse());
                    processor.processRollback(new RollbackUpdateCommand(request));
                }
                finally
                {
                    if (processor != null)
                    {
                        processor.finish();
                    }
                }
            }
            finally
            {
                activeTrackerThreadsLock.writeLock().unlock();
            }
        }
        finally
        {
            commitAndRollbackLock.writeLock().unlock();
        }
    }

    @Override
    public void registerTrackerThread()
    {
        activeTrackerThreadsLock.writeLock().lock();
        try
        {
            activeTrackerThreads.add(Thread.currentThread().getId());
        }
        finally
        {
            activeTrackerThreadsLock.writeLock().unlock();
        }
    }

    @Override
    public void unregisterTrackerThread()
    {
        activeTrackerThreadsLock.writeLock().lock();
        try
        {
            activeTrackerThreads.remove(Thread.currentThread().getId());
        }
        finally
        {
            activeTrackerThreadsLock.writeLock().unlock();
        }
    }

    @Override
    public void reindexNodeByQuery(String query) throws IOException, JSONException
    {
        RefCounted<SolrIndexSearcher> refCounted = null;
        try (SolrQueryRequest request = newSolrQueryRequest())
        {
            refCounted = core.getSearcher(false, true, null);
            SolrIndexSearcher solrIndexSearcher = refCounted.get();

            NumericDocValues dbidDocValues = solrIndexSearcher.getSlowAtomicReader().getNumericDocValues(QueryConstants.FIELD_DBID);

            List<Node> batch = new ArrayList<>(200);
            DocList docList = cloud.getDocList(nativeRequestHandler, request, query.startsWith("{") ? query : "{!afts}"+query);
            for (DocIterator it = docList.iterator(); it.hasNext(); /**/)
            {
                int docID = it.nextDoc();
                // Obtain the ACL ID for this ACL doc.
                long dbid = dbidDocValues.get(docID);

                Node node = new Node();
                node.setId(dbid);
                node.setStatus(SolrApiNodeStatus.UNKNOWN);
                node.setTxnId(Long.MAX_VALUE);

                batch.add(node);

                if(batch.size() >= 200)
                {
                    indexNodes(batch, true);
                    batch.clear();
                }
            }
            if(batch.size() > 0)
            {
                indexNodes(batch, true);
                batch.clear();
            }
        }
        finally
        {
            ofNullable(refCounted).ifPresent(RefCounted::decref);
        }
    }

    @Override
    public String getBaseUrl()
    {
        return baseUrl;
    }

    @Override
    public int getPort()
    {
        return port;
    }

    @Override
    public String getHostName()
    {
       return hostName;
    }

    @Override
    public void setCleanContentTxnFloor(long cleanContentTxnFloor)
    {
        // Nothing to be done here
    }

    @Override
    public void setCleanCascadeTxnFloor(long cleanCascadeTxnFloor)
    {
        // Nothing to be done here
    }

    public Properties getProps()
    {
        return props;
    }

    private void putTransactionState(UpdateRequestProcessor processor, SolrQueryRequest request, Transaction tx) throws IOException
    {
        SolrDocument txState = getState(core, request, "TRACKER!STATE!TX");
        String version = version(txState, FIELD_S_TXCOMMITTIME, FIELD_S_TXID, tx.getCommitTimeMs(), tx.getId());

        if (version != null)
        {
            SolrInputDocument input = new SolrInputDocument();
            input.addField(FIELD_SOLR4_ID, "TRACKER!STATE!TX");
            input.addField(FIELD_VERSION, version);
            input.addField(FIELD_S_TXID, tx.getId());
            input.addField(FIELD_S_INTXID, tx.getId());
            input.addField(FIELD_S_TXCOMMITTIME, tx.getCommitTimeMs());
            input.addField(FIELD_DOC_TYPE, DOC_TYPE_STATE);

            AddUpdateCommand cmd = new AddUpdateCommand(request);
            cmd.overwrite = true;
            cmd.solrDoc = input;
            processor.processAdd(cmd);
        }
    }

    private boolean isInIndexImpl(String ids)
    {
        try (SolrQueryRequest request = newSolrQueryRequest())
        {
            SolrRequestHandler handler = core.getRequestHandler(REQUEST_HANDLER_GET);
            ModifiableSolrParams newParams =
                    new ModifiableSolrParams(request.getParams())
                            .set("ids", ids);
            request.setParams(newParams);

            return executeQueryRequest(request, newSolrQueryResponse(), handler).getNumFound() > 0;
        }
    }

    private void canUpdate()
    {
        activeTrackerThreadsLock.readLock().lock();
        try
        {
            if (!activeTrackerThreads.contains(Thread.currentThread().getId()))
            {
                throw new TrackerStateException(
                        "The trackers work was rolled back by another tracker error. The original cause has been dumped previously in the log.");
            }
        }
        finally
        {
            activeTrackerThreadsLock.readLock().unlock();
        }
    }

    private String baseUrl(Properties properties)
    {
        return ofNullable(ConfigUtil.locateProperty(SOLR_BASEURL, properties.getProperty(SOLR_BASEURL)))
                .map(value -> (value.startsWith("/") ? "" : "/") + value + "/" + core.getName() + "/")
                .orElse(null);
    }

    private int portNumber(Properties properties)
    {
        String portNumber =
                ofNullable(ConfigUtil.locateProperty(SOLR_PORT, properties.getProperty(SOLR_PORT)))
                        .filter(value -> value.trim().length() > 0)
                        .orElseGet(() -> {
                            String jettyPort = System.getProperty("jetty.port");
                            LOGGER.debug("Using jetty.port ", jettyPort);
                            return jettyPort;
                        });

        return ofNullable(portNumber)
                .map(value -> {
                    try
                    {
                        return Integer.parseInt(value);
                    } catch (NumberFormatException exception)
                    {
                        LOGGER.error("Failed to find a valid solr.port number, the default value is in shared.properties");
                        throw exception;
                    } })
                .orElse(0);
    }

    private boolean isInIndex(long id, LRU cache, String fieldName, boolean populateCache, SolrCore core) throws IOException
    {
        if(cache.containsKey(id))
        {
            return true;
        }
        else
        {
            RefCounted<SolrIndexSearcher> refCounted = null;
            try
            {
                if(populateCache)
                {
                    cache.put(id, null); // Safe to add this here because we reset this on rollback.
                }
                refCounted = core.getSearcher();
                SolrIndexSearcher searcher = refCounted.get();
                FieldType fieldType = searcher.getSchema().getField(fieldName).getType();
                TermQuery q = new TermQuery(new Term(fieldName, fieldType.readableToIndexed(Long.toString(id))));
                TopDocs topDocs = searcher.search(q, 1);
                return topDocs.totalHits > 0;
            }
            finally
            {
                ofNullable(refCounted).ifPresent(RefCounted::decref);
            }
        }
    }

    private SolrDocumentList executeQueryRequest(SolrQueryRequest request, SolrQueryResponse response, SolrRequestHandler handler)
    {
        handler.handleRequest(request, response);

        NamedList<?> values = response.getValues();
        return (SolrDocumentList) values.get(RESPONSE_DEFAULT_IDS);
    }

    /**
     * Adds tenant information to an authority, <strong>if required</strong>, such that jbloggs for tenant example.com
     * would become jbloggs@example.com
     *
     * @param authority   The authority to mutate, if it matches certain types.
     * @param tenant      The tenant that will be added to the authority.
     * @return The new authority information
     */
    private String addTenantToAuthority(String authority, String tenant)
    {
        AuthorityType authorityType = AuthorityType.getAuthorityType(authority);
        if ((authorityType == GROUP || authorityType == EVERYONE || authorityType == GUEST) && !tenant.isEmpty())
        {
            return authority + "@" + tenant;
        }
        return authority;
    }

    /**
     * Creates a basic SolrInputDocument from the input metadata and with the given type (docType).
     * Note the supplier argument could provide a full or {@link PartialSolrInputDocument} document instance.
     *
     * @param metadata the source node metadata.
     * @param docType the document type.
     * @param initialEmptyDocumentSupplier a factory for creating the initial {@link SolrInputDocument} instance.
     * @return a basic {@link SolrInputDocument} instance populated with the minimal set of information.
     */
    private <T extends SolrInputDocument> T basicDocument(NodeMetaData metadata, String docType, Supplier<T> initialEmptyDocumentSupplier)
    {
        T doc = initialEmptyDocumentSupplier.get();
        doc.setField(FIELD_SOLR4_ID,
                AlfrescoSolrDataModel.getNodeDocumentId(
                        metadata.getTenantDomain(),
                        metadata.getId()));
        doc.setField(FIELD_VERSION, 0);

        // Here is used add in order to make sure that the atomic update happens
        doc.removeField(FIELD_DBID);
        doc.addField(FIELD_DBID, metadata.getId());

        doc.setField(FIELD_LID, metadata.getNodeRef().toString());
        doc.setField(FIELD_INTXID, metadata.getTxnId());
        doc.setField(FIELD_DOC_TYPE, docType);
        doc.setField(FIELD_ACLID, metadata.getAclId());
        return doc;
    }

    protected void updatePathRelatedFields(NodeMetaData nodeMetaData, SolrInputDocument doc)
    {
        clearFields(doc, FIELD_PATH, FIELD_SITE, FIELD_TAG, FIELD_TAG_SUGGEST, FIELD_APATH, FIELD_ANAME);

        boolean repoOnly = true;
        for (Pair<String, QName> path : nodeMetaData.getPaths())
        {
            doc.addField(FIELD_PATH, path.getFirst());

            Matcher matcher = CAPTURE_SITE.matcher(path.getFirst());
            if(matcher.find())
            {
                repoOnly = false;
                doc.addField(FIELD_SITE, ISO9075.decode(matcher.group(1)));
            }

            matcher = CAPTURE_SHARED_FILES.matcher(path.getFirst());
            if(matcher.find())
            {
                repoOnly = false;
                doc.addField(FIELD_SITE, SHARED_FILES);
            }

            matcher = CAPTURE_TAG.matcher(path.getFirst());
            if(matcher.find())
            {
                String tag = ISO9075.decode(matcher.group(1));
                doc.addField(FIELD_TAG, tag);
                doc.addField(FIELD_TAG_SUGGEST, tag);
            }
        }

        if(repoOnly)
        {
            doc.addField(FIELD_SITE, NO_SITE);
        }

        // Saving calculated APATH and ANAME elements in order to avoid storing duplicate values
        Set<String> addedAPaths = new HashSet<>();
        Set<String> addedANames = new HashSet<>();
        notNullOrEmpty(nodeMetaData.getAncestorPaths())
            .forEach(ancestorPath -> {
                String [] elements =
                        stream(notNullOrEmpty(ancestorPath.length() > 0 && ancestorPath.startsWith("/")
                                ? ancestorPath.substring(1).split("/")
                                : ancestorPath.split("/")))
                                .map(String::trim)
                                .toArray(String[]::new);

                StringBuilder builder = new StringBuilder();
                int i = 0;
                for (String element : elements)
                {
                    builder.append('/').append(element);
                    String apath = "" + i++ + builder;
                    if (!addedAPaths.contains(apath))
                    {
                        doc.addField(FIELD_APATH, apath);
                        addedAPaths.add(apath);
                    }
                }

                if (builder.length() > 0)
                {
                    doc.addField(FIELD_APATH, "F" + builder);
                }

                builder = new StringBuilder();
                for (int j = 0;  j < elements.length; j++)
                {
                    String element = elements[elements.length - 1 - j];
                    builder.insert(0, element);
                    builder.insert(0, '/');
                    String aname = "" + j +  builder;
                    if (!addedANames.contains(aname))
                    {
                        doc.addField(FIELD_ANAME, aname);
                        addedANames.add(aname);
                    }
                }

                if (builder.length() > 0)
                {
                    doc.addField(FIELD_ANAME, "F" +  builder);
                }
            });
    }

    private void cascadeUpdateV2(
            NodeMetaData parentNodeMetaData,
            boolean overwrite,
            SolrQueryRequest request,
            UpdateRequestProcessor processor) throws IOException, JSONException
    {
        RefCounted<SolrIndexSearcher> refCounted = null;
        IntArrayList docList;
        Set<Long> childIds = new HashSet<>();

        try
        {
            refCounted = core.getSearcher();
            SolrIndexSearcher searcher = refCounted.get();
            BooleanQuery.Builder builder = new BooleanQuery.Builder();
            TermQuery termQuery = new TermQuery(new Term(FIELD_ANCESTOR, parentNodeMetaData.getNodeRef().toString()));
            BooleanClause booleanClause = new BooleanClause(termQuery, BooleanClause.Occur.MUST);
            builder.add(booleanClause);
            BooleanQuery booleanQuery = builder.build();
            DocListCollector collector = new DocListCollector();
            searcher.search(booleanQuery, collector);
            docList = collector.getDocs();
            int size = docList.size();
            for(int i=0; i<size; i++)
            {
                int docId = docList.get(i);
                Document document = searcher.doc(docId, REQUEST_ONLY_ID_FIELD);
                IndexableField indexableField = document.getField(FIELD_SOLR4_ID);
                String id = indexableField.stringValue();
                TenantDbId ids = AlfrescoSolrDataModel.decodeNodeDocumentId(id);
                childIds.add(ids.dbId);
            }
        }
        finally
        {
            ofNullable(refCounted).ifPresent(RefCounted::decref);
        }

        for (Long childId : childIds)
        {
            NodeMetaDataParameters nmdp = new NodeMetaDataParameters();
            nmdp.setFromNodeId(childId);
            nmdp.setToNodeId(childId);
            nmdp.setIncludeAclId(true);
            nmdp.setIncludeAspects(false);
            nmdp.setIncludeChildAssociations(false);
            nmdp.setIncludeChildIds(true);
            nmdp.setIncludeNodeRef(true);
            nmdp.setIncludeOwner(false);
            nmdp.setIncludeParentAssociations(false);

            // We only care about the path and ancestors (which is included) for this case
            nmdp.setIncludePaths(true);
            nmdp.setIncludeProperties(false);
            nmdp.setIncludeType(true);
            nmdp.setIncludeTxnId(true);
            nmdp.setMaxResults(1);
            // Gets only one
            Optional<Collection<NodeMetaData>> nodeMetaDatas = getNodesMetaDataFromRepository(nmdp);

            if (nodeMetaDatas.isPresent() && !nodeMetaDatas.get().isEmpty())
            {
                NodeMetaData nodeMetaData = nodeMetaDatas.get().iterator().next();

                // Only cascade update nods we know can not have changed and must be in this shard
                // Node in the current TX will be explicitly updated in the outer loop
                // We do not bring in changes from the future as nodes may switch shards and we do not want the logic here.
                if (nodeMetaData.getTxnId() < parentNodeMetaData.getTxnId())
                {
                    LOGGER.debug("Cascade update child doc {}", childId);

                    SolrInputDocument document = basicDocument(nodeMetaData, DOC_TYPE_NODE, PartialSolrInputDocument::new);

                    AddUpdateCommand addDocCmd = new AddUpdateCommand(request);
                    addDocCmd.overwrite = overwrite;
                    addDocCmd.solrDoc = document;
                    if (cascadeTrackingEnabled())
                    {
                        updatePathRelatedFields(nodeMetaData, document);
                        updateNamePathRelatedFields(nodeMetaData, document);
                        updateAncestorRelatedFields(nodeMetaData, document);
                    }
                        processor.processAdd(addDocCmd);
                }
            }
        }
    }

    private long topNodeId(SolrQuery.ORDER order)
    {
        final String sortDir = order.name();
        try (SolrQueryRequest request = this.newSolrQueryRequest())
        {
            ModifiableSolrParams queryParams =
                    new ModifiableSolrParams(request.getParams())
                            .set(CommonParams.Q, "*:*")
                            .set(CommonParams.FQ, FIELD_DOC_TYPE + ":" + DOC_TYPE_NODE)
                            .set(CommonParams.ROWS, 1)
                            .set(CommonParams.SORT, FIELD_DBID + " " + sortDir)
                            .set(CommonParams.FL, FIELD_DBID);

            return notNullOrEmpty(cloud.getSolrDocumentList(nativeRequestHandler, request, queryParams))
                    .stream()
                    .findFirst()
                    .map(doc -> getFieldValueLong(doc, FIELD_DBID))
                    .orElse(0L);
        }
    }

    private void initSkippingDescendantDocs(Properties p, Set<QName> dataForSkippingDescendantDocs, String propPrefixParent,
                                            String skipByField, DefinitionExistChecker dec)
    {
        int i = 0;
        for (String key = new StringBuilder(propPrefixParent).append(i).toString(); p.containsKey(key);
             key = new StringBuilder(propPrefixParent).append(++i).toString())
        {
            String qNameInString = p.getProperty(key);
            if ((null != qNameInString) && !qNameInString.isEmpty())
            {
                QName qName = QName.resolveToQName(dataModel.getNamespaceDAO(), qNameInString);

                LOGGER.warning("QName was not found for {}", qNameInString);

                if (dec.isDefinitionExists(qName))
                {
                    dataForSkippingDescendantDocs.add(qName);
                }
            }
        }

        if (null == skippingDocsQueryString)
        {
            skippingDocsQueryString = this.cloud.getQuery(skipByField, OR, dataForSkippingDescendantDocs);
        }
        else
        {
            skippingDocsQueryString += OR + this.cloud.getQuery(skipByField, OR, dataForSkippingDescendantDocs);
        }
    }

    private void reportTransactionInfo(TransactionInfoReporter reporter, Long minId, long maxId, IOpenBitSet idsInDb,
                                       SolrQueryRequest request, String field)
    {
        if (minId != null)
        {
            IOpenBitSet idsInIndex = this.getOpenBitSetInstance();
            long batchStartId = minId;
            long batchEndId = Math.min(batchStartId + BATCH_FACET_TXS, maxId);

            // Continues as long as the batch does not pass the maximum
            while (batchStartId <= maxId)
            {
                long iterationStart = batchStartId;
                NamedList<Integer> idCounts = this.getFacets(request,
                        field + ":[" + batchStartId + " TO " + batchEndId + "]",
                        field, 1); // Min count of 1 ensures that the id returned is in the index
                for (Map.Entry<String, Integer> idCount : idCounts)
                {
                    long idInIndex = Long.parseLong(idCount.getKey());

                    // Only looks at facet values that fit the query
                    if (batchStartId <= idInIndex && idInIndex <= batchEndId)
                    {
                        idsInIndex.set(idInIndex);

                        // The sequence of ids in the index could look like this: 1, 2, 5, 7...
                        for (long id = iterationStart; id <= idInIndex; id++)
                        {
                            if (id == idInIndex)
                            {
                                iterationStart = idInIndex + 1;

                                if (!idsInDb.get(id))
                                {
                                    reporter.reportIdInIndexButNotInDb(id);
                                }
                            }
                            else if (idsInDb.get(id))
                            {
                                reporter.reportIdInDbButNotInIndex(id);
                            }
                        }

                        if (idCount.getValue() > 1)
                        {
                            reporter.reportDuplicatedIdInIndex(idInIndex);
                        }
                    }
                    else
                    {
                        break;
                    }
                }

                batchStartId = batchEndId + 1;
                batchEndId = Math.min(batchStartId + BATCH_FACET_TXS, maxId);
            }

            reporter.reportUniqueIdsInIndex(idsInIndex.cardinality());
        }
    }

    private void setDuplicates(IndexHealthReport report, SolrQueryRequest request, String docType,
                               SetDuplicatesCommand cmd)
    {
        // A mincount of 2 checks for duplicates in solr
        NamedList<Integer> dbIdCounts = getFacets(request, FIELD_DOC_TYPE + ":" + docType, FIELD_DBID, 2);
        for (Map.Entry<String, Integer> dbId : dbIdCounts)
        {
            long duplicatedDbId = Long.parseLong(dbId.getKey());
            cmd.execute(report, duplicatedDbId);
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private NamedList<Integer> getFacets(SolrQueryRequest request, String query, String field, int minCount)
    {
        ModifiableSolrParams params =
                new ModifiableSolrParams(request.getParams())
                        .set(CommonParams.Q, query)
                        .set(CommonParams.ROWS, 0)
                        .set(FacetParams.FACET, true)
                        .set(FacetParams.FACET_FIELD, field)
                        .set(FacetParams.FACET_MINCOUNT, minCount);

        SolrQueryResponse response = cloud.getResponse(nativeRequestHandler, request, params);
        NamedList facetCounts = (NamedList) response.getValues().get("facet_counts");
        NamedList facetFields = (NamedList) facetCounts.get("facet_fields");
        return (NamedList) facetFields.get(field);
    }

    public int getDocListSize(String query)
    {
        try (SolrQueryRequest request = this.newSolrQueryRequest())
        {
            ModifiableSolrParams params =
                    new ModifiableSolrParams(request.getParams())
                            .set(CommonParams.Q, query)
                            .set(CommonParams.ROWS, 0);
            ResultContext resultContext = cloud.getResultContext(nativeRequestHandler, request, params);
            return resultContext.getDocList().matches();
        }
    }

    private String getFieldValueString(SolrDocument doc, String fieldName)
    {
        IndexableField field = (IndexableField)doc.getFieldValue(fieldName);
        String value = null;
        if (field != null)
        {
            value = field.stringValue();
        }
        return value;
    }

    private long getFieldValueLong(SolrDocument doc, String fieldName)
    {
        return Long.parseLong(getFieldValueString(doc, fieldName));
    }

    private Query documentsWithOutdatedContentQuery()
    {
        Query onlyDocumentsWhoseContentNeedsToBeUpdated =
                LegacyNumericRangeQuery.newLongRange(
                        LAST_INCOMING_CONTENT_VERSION_ID,
                        CONTENT_OUTDATED_MARKER,
                        CONTENT_OUTDATED_MARKER,
                        true,
                        true);
        Query onlyDocumentsThatRepresentNodes = new TermQuery(new Term(FIELD_DOC_TYPE, DOC_TYPE_NODE));

        return new BooleanQuery.Builder()
                    .add(onlyDocumentsWhoseContentNeedsToBeUpdated, BooleanClause.Occur.MUST)
                    .add(onlyDocumentsThatRepresentNodes, BooleanClause.Occur.MUST)
                    .build();
    }

    private void deleteById(String field, Long id) throws IOException
    {
        String query = field + ":" + id;
        deleteByQuery(query);
    }

    private void deleteByQuery(String query) throws IOException
    {
        UpdateRequestProcessor processor = null;
        try (SolrQueryRequest request = newSolrQueryRequest())
        {
            processor = this.core.getUpdateProcessingChain(null).createProcessor(request, newSolrQueryResponse());
            DeleteUpdateCommand delDocCmd = new DeleteUpdateCommand(request);
            delDocCmd.setQuery(query);
            processor.processDelete(delDocCmd);
        }
        finally
        {
            if (processor != null)
            {
                processor.finish();
            }
        }
    }

    SolrDocument getState(SolrCore core, SolrQueryRequest request, String id)
    {
        ModifiableSolrParams newParams =
                new ModifiableSolrParams(request.getParams())
                        .set(CommonParams.ID, id);
        request.setParams(newParams);

        SolrQueryResponse response = newSolrQueryResponse();
        core.getRequestHandler(REQUEST_HANDLER_GET).handleRequest(request, response);

        NamedList<?> values = response.getValues();
        return (SolrDocument)values.get(RESPONSE_DEFAULT_ID);
    }

    SolrQueryResponse newSolrQueryResponse()
    {
        return new SolrQueryResponse();
    }

    LocalSolrQueryRequest newSolrQueryRequest()
    {
        return new LocalSolrQueryRequest(core, new NamedList<>());
    }

    private String version(SolrDocument stateDoc, String txCommitTimeField, String txIdField, long commitTime, long id)
    {
        if (stateDoc == null) return "0";

        long txCommitTime = getFieldValueLong(stateDoc, txCommitTimeField);
        long txId = getFieldValueLong(stateDoc, txIdField);

        return (commitTime >= txCommitTime && id > txId) ? this.getFieldValueString(stateDoc, FIELD_VERSION) : null;
    }

    private void putAclTransactionState(UpdateRequestProcessor processor, SolrQueryRequest request, AclChangeSet changeSet) throws IOException
    {
        SolrDocument aclState = getState(core, request, "TRACKER!STATE!ACLTX");
        String version = version(aclState, FIELD_S_ACLTXCOMMITTIME, FIELD_S_ACLTXID, changeSet.getCommitTimeMs(), changeSet.getId());

        if (version != null)
        {

            SolrInputDocument input = new SolrInputDocument();
            input.addField(FIELD_SOLR4_ID, "TRACKER!STATE!ACLTX");
            input.addField(FIELD_VERSION, version);
            input.addField(FIELD_S_ACLTXID, changeSet.getId());
            input.addField(FIELD_S_INACLTXID, changeSet.getId());
            input.addField(FIELD_S_ACLTXCOMMITTIME, changeSet.getCommitTimeMs());
            input.addField(FIELD_DOC_TYPE, DOC_TYPE_STATE);

            AddUpdateCommand cmd = new AddUpdateCommand(request);
            cmd.overwrite = true;
            cmd.solrDoc = input;
            processor.processAdd(cmd);
        }
    }

    private String getLocalisedValue(StringPropertyValue property, PropertyValue localeProperty)
    {
        Locale locale =
                ofNullable(localeProperty)
                        .map(StringPropertyValue.class::cast)
                        .map(StringPropertyValue::getValue)
                        .map(value -> DefaultTypeConverter.INSTANCE.convert(Locale.class, value))
                        .orElse(I18NUtil.getLocale());

        return "\u0000" + locale.getLanguage() + "\u0000" + property.getValue();
    }

    private void addStringProperty(BiConsumer<String, Object> consumer, FieldInstance field, StringPropertyValue property, PropertyValue localeProperty)
    {
        consumer.accept(field.getField(), field.isLocalised() ? getLocalisedValue(property, localeProperty) : property.getValue());
    }

    /**
     * Checks if the given property definition refers to a field that can be destructured in parts.
     * A field can be destructured only if:
     *
     * <ul>
     *     <li>It is not multivalued</li>
     *     <li>Destructuring is not disabled in configuration</li>
     *     <li>Its data type is {@link DataTypeDefinition#DATE} or {@link DataTypeDefinition#DATETIME}</li>
     * </ul>
     */
    boolean canBeDestructured(PropertyDefinition definition, String fieldName)
    {
        return !definition.isMultiValued() &&
                fieldName != null &&
                fieldName.contains("@") && // avoid static date/datetime fields
                dateFieldDestructuringHasBeenEnabledOnThisInstance &&
                (definition.getDataType().getName().equals(DataTypeDefinition.DATETIME) ||
                        definition.getDataType().getName().equals(DataTypeDefinition.DATE));
    }

    /**
     * To add support for SQL date functions SEARCH-2171 introduced date and datetime fields destructuration.
     * Other than being indexed as plain TrieDate fields, date and datetime fields are also indexed in separate fields that
     * contain their constituent parts.
     *
     * Date fields:
     *
     * <ul>
     *     <li>YEAR</li>
     *     <li>QUARTER (1-4)</li>
     *     <li>MONTH (1-12)</li>
     *     <li>DAY (OF THE MONTH)</li>
     * </ul>
     *
     * Datetime fields also add the following:
     *
     * <ul>
     *     <li>HOUR (0-23)</li>
     *     <li>MINUTE</li>
     *     <li>SECOND</li>
     * </ul>
     *
     * Note the destructured parts are not localised to a specific Timezone: they are always expressed in UTC.
     *
     * @see <a href="https://issues.alfresco.com/jira/browse/SEARCH-2171">SEARCH-2171</a>
     */
    void setUnitOfTimeFields(BiConsumer<String, Object> consumer, String sourceFieldName, String value, DataTypeDefinition dataType)
    {
        try
        {
            String fieldNamePrefix = dataModel.destructuredDateTimePartFieldNamePrefix(sourceFieldName);
            ZonedDateTime dateTime = ZonedDateTime.parse(value, DateTimeFormatter.ISO_ZONED_DATE_TIME);

            consumer.accept(fieldNamePrefix + UNIT_OF_TIME_YEAR_FIELD_SUFFIX, dateTime.getYear());
            consumer.accept(fieldNamePrefix + UNIT_OF_TIME_QUARTER_FIELD_SUFFIX, dateTime.get(IsoFields.QUARTER_OF_YEAR));
            consumer.accept(fieldNamePrefix + UNIT_OF_TIME_MONTH_FIELD_SUFFIX, dateTime.getMonth().getValue());
            consumer.accept(fieldNamePrefix + UNIT_OF_TIME_DAY_FIELD_SUFFIX, dateTime.getDayOfMonth());
            consumer.accept(fieldNamePrefix + UNIT_OF_TIME_DAY_OF_WEEK_FIELD_SUFFIX, dateTime.getDayOfWeek().getValue());
            consumer.accept(fieldNamePrefix + UNIT_OF_TIME_DAY_OF_YEAR_FIELD_SUFFIX, dateTime.getDayOfYear());

            if (DataTypeDefinition.DATETIME.equals(dataType.getName()) && isTimeComponentDefined(value))
            {
                consumer.accept(fieldNamePrefix + UNIT_OF_TIME_MINUTE_FIELD_SUFFIX, dateTime.getMinute());
                consumer.accept(fieldNamePrefix + UNIT_OF_TIME_HOUR_FIELD_SUFFIX, dateTime.getHour());
                consumer.accept(fieldNamePrefix + UNIT_OF_TIME_SECOND_FIELD_SUFFIX, dateTime.getSecond());
            }
        }
        catch (Exception exception)
        {
            LOGGER.error("Unable to destructure date/datetime value {} (Field was {})", value, sourceFieldName, exception);
        }
    }

    private void addFieldIfNotSet(SolrInputDocument doc, String name)
    {
        doc.addField(FIELD_FIELDS, name);
    }

    private boolean mayHaveChildren(NodeMetaData nodeMetaData)
    {
        return ofNullable(nodeMetaData)
                    .map(metadata ->
                            nodeTypeSupportsChildren(metadata) ||
                            atLeastOneAspectSupportsChildren(metadata.getAspects()))
                    .orElse(false);
    }

    private boolean nodeTypeSupportsChildren(NodeMetaData metadata)
    {
        return ofNullable(dataModel.getDictionaryService(CMISStrictDictionaryService.DEFAULT))
                .map(comp -> comp.getType(metadata.getType()))
                .map(TypeDefinition::getChildAssociations)
                .map(associations -> !associations.isEmpty())
                .orElse(false);
    }

    private boolean atLeastOneAspectSupportsChildren(Set<QName> aspects)
    {
        return notNullOrEmpty(aspects).stream()
                .map(aspect -> dataModel.getDictionaryService(CMISStrictDictionaryService.DEFAULT).getAspect(aspect))
                .filter(Objects::nonNull)
                .map(AspectDefinition::getChildAssociations)
                .filter(Objects::nonNull)
                .anyMatch(associations -> !associations.isEmpty());
    }

    private long getSafeCount(NamedList<Integer> counts, String countType)
    {
        return ofNullable(counts)
                .map(container -> container.get(countType))
                .orElse(0);
    }

    private NamedList<Object> fixStats(NamedList<Object> namedList)
    {
        int sz = namedList.size();

        for (int i = 0; i < sz; i++)
        {
            Object value = namedList.getVal(i);
            if (value instanceof Number)
            {
                Number number = (Number) value;
                if (Float.isInfinite(number.floatValue()) || Float.isNaN(number.floatValue())
                        || Double.isInfinite(number.doubleValue()) || Double.isNaN(number.doubleValue()))
                {
                    namedList.setVal(i, null);
                }
            }
        }
        return namedList;
    }

    private List<SolrIndexSearcher> getRegisteredSearchers()
    {
        List<SolrIndexSearcher> searchers = new ArrayList<>();
        for (Entry<String, SolrInfoMBean> entry : core.getInfoRegistry().entrySet())
        {
            if (entry.getValue() != null)
            {
                if (entry.getValue().getName().equals(SolrIndexSearcher.class.getName()))
                {
                    if (!entry.getKey().equals("searcher"))
                    {
                        searchers.add((SolrIndexSearcher) entry.getValue());
                    }
                }
            }
        }
        return searchers;
    }

    private void clearFields(SolrInputDocument document, List<String> fields)
    {
        notNullOrEmpty(fields).forEach(document::removeField);
    }

    private void clearFields(SolrInputDocument document, String... fields)
    {
        stream(notNullOrEmpty(fields)).forEach(document::removeField);
    }

    private NodeMetaData createDeletedNodeMetaData(Node node)
    {
        NodeMetaData nodeMetaData = new NodeMetaData();
        nodeMetaData.setId(node.getId());
        nodeMetaData.setType(ContentModel.TYPE_DELETED);
        nodeMetaData.setNodeRef(new NodeRef(node.getNodeRef()));
        nodeMetaData.setTxnId(node.getTxnId());
        return nodeMetaData;
    }

    /**
     * Get the metadata for the specified nodes from the repository.
     *
     * @param parameters A parameters object containing either a list of nodes ({@link NodeMetaDataParameters#getNodeIds})
     * or a node range ({@link NodeMetaDataParameters#getFromNodeId} and {@link NodeMetaDataParameters#getToNodeId}).
     * @return Either the metadata returned by the repository, or null if there was a problem.
     */
    private Optional<Collection<NodeMetaData>> getNodesMetaDataFromRepository(NodeMetaDataParameters parameters)
    {
        try
        {
            return Optional.of(notNullOrEmpty(repositoryClient.getNodesMetaData(parameters)));
        }
        catch (JSONException exception)
        {
            // The exception has been already logged in repositoryClient and could be huge. Simply log a reference to it here.
            LOGGER.debug("JSON exception swallowed by SolrInformationServer.");
            return empty();
        }
        catch (Exception exception)
        {
            LOGGER.error("Unable to get nodes metadata from repository using "
                    + "fromNodeId=" + parameters.getFromNodeId() + ", "
                    + "toNodeId=" + parameters.getToNodeId() + ", "
                    + "nodeIds=" + parameters.getNodeIds() +  ", "
                    + "fromTxId=" + parameters.getFromTxnId() + ", "
                    + "toTxId=" + parameters.getToTxnId() + ", "
                    + "txIds=" + parameters.getTransactionIds() 
                    + ". See the stacktrace below for further details.", exception);
            return empty();
        }
    }
}
