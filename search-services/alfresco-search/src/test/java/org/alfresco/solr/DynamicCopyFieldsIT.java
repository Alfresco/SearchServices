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

import com.google.common.collect.Sets;
import org.alfresco.service.namespace.QName;
import org.alfresco.solr.client.Acl;
import org.alfresco.solr.client.AclChangeSet;
import org.alfresco.solr.client.AclReaders;
import org.alfresco.solr.client.MLTextPropertyValue;
import org.alfresco.solr.client.Node;
import org.alfresco.solr.client.NodeMetaData;
import org.alfresco.solr.client.StringPropertyValue;
import org.alfresco.solr.client.Transaction;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.TermQuery;
import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.request.LukeRequest;
import org.apache.solr.client.solrj.response.LukeResponse;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Collections.singletonList;
import static org.alfresco.solr.AlfrescoSolrUtils.ancestors;
import static org.alfresco.solr.AlfrescoSolrUtils.getAcl;
import static org.alfresco.solr.AlfrescoSolrUtils.getAclChangeSet;
import static org.alfresco.solr.AlfrescoSolrUtils.getAclReaders;
import static org.alfresco.solr.AlfrescoSolrUtils.getNode;
import static org.alfresco.solr.AlfrescoSolrUtils.getNodeMetaData;
import static org.alfresco.solr.AlfrescoSolrUtils.getTransaction;
import static org.alfresco.solr.AlfrescoSolrUtils.indexAclChangeSet;
import static org.carrot2.shaded.guava.common.collect.ImmutableList.of;

@SolrTestCaseJ4.SuppressSSL
public class DynamicCopyFieldsIT extends AbstractAlfrescoDistributedIT {

    private static Node parentFolder;
    private static NodeMetaData parentFolderMetadata;
    private static Node node0;
    private static NodeMetaData metadataNode0;
    private static final int timeout = 100000;

    private static QName getCustomQName(String name){
        return QName.createQName("allfieldtypes", name);
    }
    private static QName ASPECT_ALL_FIELDS_TYPE = QName.createQName("http://www.alfresco.org/model/content/1.0", "allfieldtypes");
    private static final String MLTEXT_LOVPARTIAL = "mltextLOVPartial";
    private static final String TEXT_PATTERN_UNIQUE = "textPatternUnique";
    private static final String MLTEXT_FREE = "mltextFree";
    private static final String TEXT_LOVPARTIAL = "textLOVPartial";
    private static final String MLTEXT_PATTERN_UNIQUE = "mltextPatternUnique";
    private static final String MLTEXT_PATTERN_MANY = "mltextPatternMany";
    private static final String TEXT_LOVWHOLE = "textLOVWhole";
    private static final String MLTEXT_NONE = "mltextNone";
    private static final String TEXT_NONE = "textNone";
    private static final String TEXT_FREE = "textFree";
    private static final String MLTEXT_LOVWHOLE ="mltextLOVWhole";
    private static final String TEXT_PATTERN_MANY = "textPatternMany";

    private static Map<String, HashSet<String>> fieldMap;

    // Following data is used for restoring alfrescoSolrDataModel after text execution
    private static HashSet<QName> oldCrossLocaleProperties;
    private static HashSet<QName> oldCrossLocaleTypes;
    private static HashSet<QName> oldIdentifierProperties;

    @BeforeClass
    public static void initData() throws Throwable
    {
        initSolrServers(1, DynamicCopyFieldsIT.class.getSimpleName(), null);
        modifyAlfrescoSolrDataModel();
        indexNodes();
        fieldMap = getIndexedFieldsModifiers();
    }

    private static void modifyAlfrescoSolrDataModel() throws NoSuchFieldException, IllegalAccessException {
        Field crossLocalePropertiesField = AlfrescoSolrDataModel
                .getInstance()
                .getClass()
                .getDeclaredField("crossLocaleSearchProperties");

        crossLocalePropertiesField.setAccessible(true);

        Field crossLocaleTypesField = AlfrescoSolrDataModel
                .getInstance()
                .getClass()
                .getDeclaredField("crossLocaleSearchDataTypes");

        crossLocaleTypesField.setAccessible(true);

        Field identifierPropertiesField = AlfrescoSolrDataModel
                .getInstance()
                .getClass()
                .getDeclaredField("identifierProperties");

        identifierPropertiesField.setAccessible(true);

        HashSet<QName> crossLocaleProperties = (HashSet<QName>) crossLocalePropertiesField.get(AlfrescoSolrDataModel.getInstance());
        HashSet<QName> crossLocaleTypes = (HashSet<QName>) crossLocaleTypesField.get(AlfrescoSolrDataModel.getInstance());
        HashSet<QName> identifierProperties = (HashSet<QName>) identifierPropertiesField.get(AlfrescoSolrDataModel.getInstance());

        oldCrossLocaleProperties = (HashSet<QName>) crossLocaleProperties.clone();
        oldCrossLocaleTypes = (HashSet<QName>) crossLocaleTypes.clone();
        oldIdentifierProperties = (HashSet<QName>) identifierProperties.clone();

        crossLocaleTypes.clear();
        crossLocaleTypes.add(QName.createQName("http://www.alfresco.org/model/dictionary/1.0", "text"));
        crossLocaleTypes.add(QName.createQName("http://www.alfresco.org/model/dictionary/1.0", "content"));
        crossLocaleProperties.add(getCustomQName(MLTEXT_LOVPARTIAL));
        identifierProperties.add(getCustomQName(MLTEXT_FREE));
    }

    private static void indexNodes() throws Exception
    {

        AclChangeSet aclChangeSet = getAclChangeSet(1);

        Acl acl = getAcl(aclChangeSet);
        AclReaders aclReaders = getAclReaders(aclChangeSet, acl, singletonList("joel"), singletonList("phil"), null);

        indexAclChangeSet(aclChangeSet,
                of(acl),
                of(aclReaders));

        Transaction bigTxn = getTransaction(0, 2);

        /*
         * Create parent folder in the first shard
         */
        parentFolder = getNode(0, bigTxn, acl, Node.SolrApiNodeStatus.UPDATED);
        parentFolderMetadata = getNodeMetaData(parentFolder, bigTxn, acl, "elia", null, false);

        /*
         * Create first node.
         * This will be stored in the first shard (range [0-100])
         */
        node0 = getNode(99, bigTxn, acl, Node.SolrApiNodeStatus.UPDATED);
        metadataNode0 = getNodeMetaData(node0, bigTxn, acl, "elia", ancestors(parentFolderMetadata.getNodeRef()), false);
        metadataNode0.getAspects().add(ASPECT_ALL_FIELDS_TYPE);
        metadataNode0.getProperties().put(getCustomQName(TEXT_PATTERN_UNIQUE), new StringPropertyValue("value"));
        metadataNode0.getProperties().put(getCustomQName(TEXT_LOVPARTIAL), new StringPropertyValue("value"));
        metadataNode0.getProperties().put(getCustomQName(TEXT_LOVWHOLE), new StringPropertyValue("value"));
        metadataNode0.getProperties().put(getCustomQName(TEXT_NONE), new StringPropertyValue("value"));
        metadataNode0.getProperties().put(getCustomQName(TEXT_FREE), new StringPropertyValue("value"));
        metadataNode0.getProperties().put(getCustomQName(TEXT_PATTERN_MANY), new StringPropertyValue("value"));
        metadataNode0.getProperties().put(getCustomQName(MLTEXT_LOVPARTIAL), new MLTextPropertyValue(Map.of(Locale.ENGLISH,"value")));
        metadataNode0.getProperties().put(getCustomQName(MLTEXT_FREE), new MLTextPropertyValue(Map.of(Locale.ENGLISH,"value")));
        metadataNode0.getProperties().put(getCustomQName(MLTEXT_PATTERN_UNIQUE), new MLTextPropertyValue(Map.of(Locale.ENGLISH,"value")));
        metadataNode0.getProperties().put(getCustomQName(MLTEXT_PATTERN_MANY), new MLTextPropertyValue(Map.of(Locale.ENGLISH,"value")));
        metadataNode0.getProperties().put(getCustomQName(MLTEXT_NONE), new MLTextPropertyValue(Map.of(Locale.ENGLISH,"value")));
        metadataNode0.getProperties().put(getCustomQName(MLTEXT_LOVWHOLE), new MLTextPropertyValue(Map.of(Locale.ENGLISH,"value")));

        indexTransaction(bigTxn,
                of(parentFolder, node0),
                of(parentFolderMetadata, metadataNode0));

        /*
         * Get sure the nodes are indexed correctly in the shards
         */
        waitForDocCount(new TermQuery(new Term("content@s___t@{http://www.alfresco.org/model/content/1.0}content", "world")), 2, timeout);
    }

    @AfterClass
    public static void destroyData() throws NoSuchFieldException, IllegalAccessException {

        restoreAlfrescoSolrDataModel();
        dismissSolrServers();
    }

    private static void restoreAlfrescoSolrDataModel() throws NoSuchFieldException, IllegalAccessException {

        Field crossLocalePropertiesField = AlfrescoSolrDataModel
                .getInstance()
                .getClass()
                .getDeclaredField("crossLocaleSearchProperties");


        Field crossLocaleTypesField = AlfrescoSolrDataModel
                .getInstance()
                .getClass()
                .getDeclaredField("crossLocaleSearchDataTypes");


        Field identifierPropertiesField = AlfrescoSolrDataModel
                .getInstance()
                .getClass()
                .getDeclaredField("identifierProperties");

        crossLocalePropertiesField.setAccessible(true);
        crossLocaleTypesField.setAccessible(true);
        identifierPropertiesField.setAccessible(true);

        crossLocalePropertiesField.set(AlfrescoSolrDataModel.getInstance(), oldCrossLocaleProperties);
        crossLocaleTypesField.set(AlfrescoSolrDataModel.getInstance(), oldCrossLocaleTypes);
        identifierPropertiesField.set(AlfrescoSolrDataModel.getInstance(), oldIdentifierProperties);
    }


    /**
     * Test correct fields are generated:
     * Properties (mltext):
     *      indexed=BOTH
     *      facetable=TRUE
     *      cross-locale=TRUE
     */
    @Test
    public void mltextLOVPartialGeneratedFieldsTest()
    {
        HashSet<String> fieldModifiers = fieldMap.get(MLTEXT_LOVPARTIAL);
        assertAllMultiValue(fieldModifiers);
        Set<DynamicFieldType> dynamicFieldsTypes = fieldModifiers.stream()
                .map(this::modifierToDynamicFieldType)
                .collect(Collectors.toSet());
        Assert.assertEquals(Set.of(DynamicFieldType.STORED,
                DynamicFieldType.NON_TOKENIZED_CROSS_LOCALE,
                DynamicFieldType.NON_TOKENIZED_LOCALE,
                DynamicFieldType.TOKENIZED_LOCALE,
                DynamicFieldType.TOKENIZED_CROSS_LOCALE,
                DynamicFieldType.SORT), dynamicFieldsTypes);

    }

    /**
     * Test correct fields are generated:
     * Properties (text):
     *      indexed=FALSE
     *      facetable=FALSE
     *      cross-locale-type=TRUE
     */
    @Test
    public void textPatternUniqueGeneratedFieldsTest()
    {
        HashSet<String> fieldModifiers = fieldMap.get(TEXT_PATTERN_UNIQUE);
        assertAllSingleValue(fieldModifiers);
        Set<DynamicFieldType> dynamicFieldsTypes = fieldModifiers.stream()
                .map(this::modifierToDynamicFieldType)
                .collect(Collectors.toSet());
        Assert.assertEquals(Set.of(DynamicFieldType.STORED,
                DynamicFieldType.NON_TOKENIZED_LOCALE,
                DynamicFieldType.NON_TOKENIZED_CROSS_LOCALE,
                DynamicFieldType.SORT), dynamicFieldsTypes);
    }

    /**
     * Test correct fields are generated:
     * Properties (mltext):
     *      indexed=TRUE
     *      facetable=FALSE
     *      identifier=TRUE
     */
    @Test
    public void mltextFreeGeneratedFieldsTest()
    {
        HashSet<String> fieldModifiers = fieldMap.get(MLTEXT_FREE);
        assertAllMultiValue(fieldModifiers);
        Set<DynamicFieldType> dynamicFieldsTypes = fieldModifiers.stream()
                .map(this::modifierToDynamicFieldType)
                .collect(Collectors.toSet());
        Assert.assertEquals(Set.of(DynamicFieldType.STORED,
                DynamicFieldType.NON_TOKENIZED_LOCALE,
                DynamicFieldType.TOKENIZED_LOCALE), dynamicFieldsTypes);

    }

    /**
     * Test correct fields are generated:
     * Properties (text):
     *      indexed=BOTH
     *      facetable=TRUE
     *      cross-locale-type=TRUE
     */
    @Test
    public void textLOVPartialGeneratedFieldsTest()
    {
        HashSet<String> fieldModifiers = fieldMap.get(TEXT_LOVPARTIAL);
        assertAllSingleValue(fieldModifiers);
        Set<DynamicFieldType> dynamicFieldsTypes = fieldModifiers.stream()
                .map(this::modifierToDynamicFieldType)
                .collect(Collectors.toSet());
        Assert.assertEquals(Set.of(DynamicFieldType.STORED,
                DynamicFieldType. NON_TOKENIZED_CROSS_LOCALE,
                DynamicFieldType.NON_TOKENIZED_LOCALE,
                DynamicFieldType.TOKENIZED_LOCALE,
                DynamicFieldType.TOKENIZED_CROSS_LOCALE,
                DynamicFieldType.DOCVALUE,
                DynamicFieldType.SORT), dynamicFieldsTypes);

    }

    /**
     * Test correct fields are generated:
     * Properties (mltext):
     *      indexed=FALSE
     *      facetable=FALSE
     */
    @Test
    public void mltextPatternUniqueGeneratedFieldsTest()
    {
        HashSet<String> fieldModifiers = fieldMap.get(MLTEXT_PATTERN_UNIQUE);
        assertAllMultiValue(fieldModifiers);
        Set<DynamicFieldType> dynamicFieldsTypes = fieldModifiers.stream()
                .map(this::modifierToDynamicFieldType)
                .collect(Collectors.toSet());
        Assert.assertEquals(Set.of(DynamicFieldType.STORED,
                DynamicFieldType.NON_TOKENIZED_LOCALE,
                DynamicFieldType.SORT), dynamicFieldsTypes);

    }

    /**
     * Test correct fields are generated:
     * Properties (mltext):
     *      indexed=BOTH
     *      facetable=FALSE
     */
    @Test
    public void MLTextPatternManyGeneratedFieldsTest()
    {
        HashSet<String> fieldModifiers = fieldMap.get(MLTEXT_PATTERN_MANY);
        assertAllMultiValue(fieldModifiers);
        Set<DynamicFieldType> dynamicFieldsTypes = fieldModifiers.stream()
                .map(this::modifierToDynamicFieldType)
                .collect(Collectors.toSet());
        Assert.assertEquals(Set.of(DynamicFieldType.STORED,
                DynamicFieldType.NON_TOKENIZED_LOCALE,
                DynamicFieldType.TOKENIZED_LOCALE,
                DynamicFieldType.SORT), dynamicFieldsTypes);

    }

    /**
     * Test correct fields are generated:
     * Properties (text):
     *      indexed=FALSE
     *      facetable=TRUE
     *      cross-locale-type=TRUE
     */
    @Test
    public void textLOVWholeGeneratedFieldsTest()
    {
        Assert.assertFalse();
        HashSet<String> fieldModifiers = fieldMap.get(TEXT_LOVWHOLE);
        assertAllSingleValue(fieldModifiers);
        Set<DynamicFieldType> dynamicFieldsTypes = fieldModifiers.stream()
                .map(this::modifierToDynamicFieldType)
                .collect(Collectors.toSet());
        Assert.assertEquals(Set.of(DynamicFieldType.STORED,
                DynamicFieldType.NON_TOKENIZED_CROSS_LOCALE,
                DynamicFieldType. NON_TOKENIZED_LOCALE,
                DynamicFieldType.DOCVALUE,
                DynamicFieldType.SORT), dynamicFieldsTypes);
    }

    /**
     * Test correct fields are generated:
     * Properties (mltext):
     *      index=disable
     */
    @Test
    @Ignore("index enable=false is ignored")
    public void mltextNoneGeneratedFieldsTest()
    {
        HashSet<String> fieldModifiers = fieldMap.get(MLTEXT_NONE);
        assertAllMultiValue(fieldModifiers);
        Set<DynamicFieldType> dynamicFieldsTypes = fieldModifiers.stream()
                .map(this::modifierToDynamicFieldType)
                .collect(Collectors.toSet());
        Assert.assertEquals(Set.of(DynamicFieldType.STORED), dynamicFieldsTypes);
    }

    /**
     * Test correct fields are generated:
     * Properties (text):
     *      index=disable
     */
    @Test
    public void textNoneGeneratedFieldsTest()
    {
        HashSet<String> fieldModifiers = fieldMap.get(TEXT_NONE);
        assertAllSingleValue(fieldModifiers);
        Set<DynamicFieldType> dynamicFieldsTypes = fieldModifiers.stream()
                .map(this::modifierToDynamicFieldType)
                .collect(Collectors.toSet());
        Assert.assertEquals(Set.of(DynamicFieldType.STORED), dynamicFieldsTypes);
    }

    /**
     * Test correct fields are generated:
     * Properties (text):
     *      indexed=TRUE
     *      facetable=FALSE
     *      cross-locale-type=TRUE
     */
    @Test
    public void textFreeGeneratedFieldsTest()
    {
        HashSet<String> fieldModifiers = fieldMap.get(TEXT_FREE);
        assertAllSingleValue(fieldModifiers);
        Set<DynamicFieldType> dynamicFieldsTypes = fieldModifiers.stream()
                .map(this::modifierToDynamicFieldType)
                .collect(Collectors.toSet());
        Assert.assertEquals(Set.of(DynamicFieldType.STORED,
                DynamicFieldType.TOKENIZED_CROSS_LOCALE,
                DynamicFieldType.TOKENIZED_LOCALE), dynamicFieldsTypes);
    }

    /**
     * Test correct fields are generated:
     * Properties (mltext):
     *      indexed=FALSE
     *      facetable=TRUE
     */
    @Test
    public void mltextLOVWholeGeneratedFieldsTest()
    {
        HashSet<String> fieldModifiers = fieldMap.get(MLTEXT_LOVWHOLE);
        assertAllMultiValue(fieldModifiers);
        Set<DynamicFieldType> dynamicFieldsTypes = fieldModifiers.stream()
                .map(this::modifierToDynamicFieldType)
                .collect(Collectors.toSet());

        Assert.assertEquals(Set.of(DynamicFieldType.STORED,
                DynamicFieldType.NON_TOKENIZED_LOCALE,
                DynamicFieldType.SORT), dynamicFieldsTypes);
    }

    /**
     * Test correct fields are generated:
     * Properties (text):
     *      indexed=BOTH
     *      facetable=FALSE
     *      cross-locale-type=TRUE
     */
    @Test
    public void textPatternManyGeneratedFieldsTest()
    {
        HashSet<String> fieldModifiers = fieldMap.get(TEXT_PATTERN_MANY);
        assertAllSingleValue(fieldModifiers);
        Set<DynamicFieldType> dynamicFieldsTypes = fieldModifiers.stream()
                .map(this::modifierToDynamicFieldType)
                .collect(Collectors.toSet());

        Assert.assertEquals(Set.of(DynamicFieldType.STORED,
                DynamicFieldType.NON_TOKENIZED_CROSS_LOCALE,
                DynamicFieldType.NON_TOKENIZED_LOCALE,
                DynamicFieldType.TOKENIZED_CROSS_LOCALE,
                DynamicFieldType.TOKENIZED_LOCALE,
                DynamicFieldType.SORT), dynamicFieldsTypes);
    }


    public static Map<String, HashSet<String>> getIndexedFieldsModifiers() throws IOException, SolrServerException {
        SolrClient solrClient = getStandaloneClients().get(0);
        LukeRequest request = new LukeRequest();
        LukeResponse response = request.process(solrClient);
        Map<String, HashSet<String>> collect = response.getFieldInfo().keySet().stream()
                .map(k -> k.split("@|\\{|\\}"))
                .filter(e -> e.length == 5)
                .collect(Collectors.toMap(
                        a -> a[4],
                        a -> Sets.newHashSet(a[1]),
                        (value1, value2) -> {
                            value1.addAll(value2);
                            return value1;
                        }));
        return collect;
    }


    private void assertAllSingleValue(Set<String> fieldModifiers)
    {
        Assert.assertFalse("all the fields should be single value",
                fieldModifiers.stream().filter(mod -> !isSingleValue(mod)).findAny().isPresent());
    }

    private void assertAllMultiValue(Set<String> fieldModifiers)
    {
        Assert.assertFalse("all the fields should be multi value",
                fieldModifiers.stream().filter(mod -> isSingleValue(mod)).findAny().isPresent());
    }

    /**
     * Parse field modifier and produce the dynamic field type.
     * @param modifier
     * @return
     */
    private DynamicFieldType modifierToDynamicFieldType(String modifier) {

        if (modifier.contains("stored")){
            return DynamicFieldType.STORED;
        }
        else if (modifier.contains("sort"))
        {
            return DynamicFieldType.SORT;
        }
        else if (modifier.charAt(1) == 'd')
        {
            return DynamicFieldType.DOCVALUE;
        }
        else if (modifier.charAt(4) == 't')
        {
            if (modifier.charAt(3) == 'l')
            {
                return DynamicFieldType.TOKENIZED_LOCALE;
            }
            else
            {
                return DynamicFieldType.TOKENIZED_CROSS_LOCALE;
            }
        }
        else if (modifier.charAt(4) == '_')
        {
            if (modifier.charAt(3) == 'l')
            {
                return DynamicFieldType.NON_TOKENIZED_LOCALE;
            }
            else
            {
                return DynamicFieldType.NON_TOKENIZED_CROSS_LOCALE;
            }
        }

        return DynamicFieldType.NONE;
    }

    private boolean isSingleValue(String modifier)
    {
        return modifier.charAt(0) == 's';
    }

    enum DynamicFieldType
    {
        STORED,
        TOKENIZED_LOCALE,
        NON_TOKENIZED_LOCALE,
        TOKENIZED_CROSS_LOCALE,
        NON_TOKENIZED_CROSS_LOCALE,
        DOCVALUE,
        SORT,
        NONE
    }
}
