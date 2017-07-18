/*
 * #%L
 * Alfresco Solr Client
 * %%
 * Copyright (C) 2005 - 2016 Alfresco Software Limited
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
package org.alfresco.solr.client;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.AlgorithmParameters;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.alfresco.encryption.DefaultEncryptionUtils;
import org.alfresco.encryption.KeyProvider;
import org.alfresco.encryption.KeyResourceLoader;
import org.alfresco.encryption.KeyStoreParameters;
import org.alfresco.encryption.MACUtils.MACInput;
import org.alfresco.encryption.ssl.SSLEncryptionParameters;
import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.httpclient.AlfrescoHttpClient;
import org.alfresco.httpclient.AuthenticationException;
import org.alfresco.httpclient.HttpClientFactory;
import org.alfresco.httpclient.HttpClientFactory.SecureCommsType;
import org.alfresco.opencmis.dictionary.CMISDictionaryRegistry;
import org.alfresco.opencmis.dictionary.CMISStrictDictionaryService;
import org.alfresco.opencmis.mapping.CMISMapping;
import org.alfresco.opencmis.mapping.RuntimePropertyLuceneBuilderMapping;
import org.alfresco.repo.cache.MemoryCache;
import org.alfresco.repo.dictionary.CompiledModelsCache;
import org.alfresco.repo.dictionary.DictionaryComponent;
import org.alfresco.repo.dictionary.DictionaryDAOImpl;
import org.alfresco.repo.dictionary.DictionaryNamespaceComponent;
import org.alfresco.repo.dictionary.M2Model;
import org.alfresco.repo.dictionary.M2Namespace;
import org.alfresco.repo.dictionary.NamespaceDAO;
import org.alfresco.repo.i18n.StaticMessageLookup;
import org.alfresco.repo.tenant.SingleTServiceImpl;
import org.alfresco.repo.tenant.TenantService;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.DynamicallySizedThreadPoolExecutor;
import org.alfresco.util.Pair;
import org.alfresco.util.TraceableThreadFactory;
import org.alfresco.util.cache.DefaultAsynchronouslyRefreshedCacheRegistry;
import org.apache.chemistry.opencmis.commons.enums.CmisVersion;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.ByteArrayRequestEntity;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONException;

import junit.framework.TestCase;

/**
 * Tests {@link SOLRAPIClient} Note: need to make sure that source/solr/instance is on the run classpath. Note: doesn't
 * currently work, need to change to use SSL.
 * 
 * @since 4.0
 */
public class SOLRAPIClientTest extends TestCase
{
    private static Log logger = LogFactory.getLog(SOLRAPIClientTest.class);

    // private static final String TEST_MODEL = "org/alfresco/repo/dictionary/dictionarydaotest_model.xml";

    private SOLRAPIClient client;

    private SOLRAPIClient invalidKeyClient;

    private SOLRAPIClient tamperWithClient;

    private DictionaryDAOImpl dictionaryDAO;

    private CMISStrictDictionaryService cmisDictionaryService;
    
    private static final String CORENAME = "collection1";

    // private M2Model testModel;

    private void loadModel(Map<String, M2Model> modelMap, HashSet<String> loadedModels, M2Model model)
    {
        String modelName = model.getName();
        if (loadedModels.contains(modelName) == false)
        {
            for (M2Namespace importNamespace : model.getImports())
            {
                M2Model importedModel = modelMap.get(importNamespace.getUri());
                if (importedModel != null)
                {

                    // Ensure that the imported model is loaded first
                    loadModel(modelMap, loadedModels, importedModel);
                }
            }

            dictionaryDAO.putModelIgnoringConstraints(model);
            loadedModels.add(modelName);
        }
    }

    @Override
    public void setUp() throws Exception
    {
        if(client == null)
        {
            TenantService tenantService = new SingleTServiceImpl();

            dictionaryDAO = new DictionaryDAOImpl();
            NamespaceDAO namespaceDAO = dictionaryDAO;
            dictionaryDAO.setTenantService(tenantService);
            
            CompiledModelsCache compiledModelsCache = new CompiledModelsCache();
            compiledModelsCache.setDictionaryDAO(dictionaryDAO);
            compiledModelsCache.setTenantService(tenantService);
            compiledModelsCache.setRegistry(new DefaultAsynchronouslyRefreshedCacheRegistry());
            TraceableThreadFactory threadFactory = new TraceableThreadFactory();
            threadFactory.setThreadDaemon(true);
            threadFactory.setThreadPriority(Thread.NORM_PRIORITY);
            
            ThreadPoolExecutor threadPoolExecutor = new DynamicallySizedThreadPoolExecutor(20, 20, 90, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(), threadFactory,
                    new ThreadPoolExecutor.CallerRunsPolicy());
            compiledModelsCache.setThreadPoolExecutor(threadPoolExecutor);
            dictionaryDAO.setDictionaryRegistryCache(compiledModelsCache);
            // TODO: use config ....
            dictionaryDAO.setDefaultAnalyserResourceBundleName("alfresco/model/dataTypeAnalyzers");
            dictionaryDAO.setResourceClassLoader(getResourceClassLoader());
            dictionaryDAO.init();

            DictionaryComponent dictionaryComponent = new DictionaryComponent();
            dictionaryComponent.setDictionaryDAO(dictionaryDAO);
            dictionaryComponent.setMessageLookup(new StaticMessageLookup());

            // cmis dictionary
            CMISMapping cmisMapping = new CMISMapping();
            cmisMapping.setCmisVersion(CmisVersion.CMIS_1_0);
            DictionaryNamespaceComponent namespaceService = new DictionaryNamespaceComponent();
            namespaceService.setNamespaceDAO(namespaceDAO);
            cmisMapping.setNamespaceService(namespaceService);
            cmisMapping.setDictionaryService(dictionaryComponent);
            cmisMapping.afterPropertiesSet();

            cmisDictionaryService = new CMISStrictDictionaryService();
            cmisDictionaryService.setCmisMapping(cmisMapping);
            cmisDictionaryService.setDictionaryService(dictionaryComponent);
            cmisDictionaryService.setDictionaryDAO(dictionaryDAO);
            cmisDictionaryService.setSingletonCache(new MemoryCache<String, CMISDictionaryRegistry>());
            cmisDictionaryService.setTenantService(tenantService);
            cmisDictionaryService.init();

            RuntimePropertyLuceneBuilderMapping luceneBuilderMapping = new RuntimePropertyLuceneBuilderMapping();
            luceneBuilderMapping.setDictionaryService(dictionaryComponent);
            luceneBuilderMapping.setCmisDictionaryService(cmisDictionaryService);
            cmisDictionaryService.setPropertyLuceneBuilderMapping(luceneBuilderMapping);

            luceneBuilderMapping.afterPropertiesSet();

            // Load the key store from the classpath
            ClasspathKeyResourceLoader keyResourceLoader = new ClasspathKeyResourceLoader();
            client = new SOLRAPIClient(getRepoClient(keyResourceLoader), dictionaryComponent, dictionaryDAO);
            trackModels();
        }
    }

    private void trackModels() throws AuthenticationException, IOException, JSONException
    {

        List<AlfrescoModelDiff> modelDiffs = client.getModelsDiff(CORENAME,Collections.<AlfrescoModel> emptyList());
        HashMap<String, M2Model> modelMap = new HashMap<String, M2Model>();

        for (AlfrescoModelDiff modelDiff : modelDiffs)
        {
            switch (modelDiff.getType())
            {
            case CHANGED:
                AlfrescoModel changedModel = client.getModel(CORENAME, modelDiff.getModelName());
                for (M2Namespace namespace : changedModel.getModel().getNamespaces())
                {
                    modelMap.put(namespace.getUri(), changedModel.getModel());
                }
                break;
            case NEW:
                AlfrescoModel newModel = client.getModel(CORENAME, modelDiff.getModelName());
                for (M2Namespace namespace : newModel.getModel().getNamespaces())
                {
                    modelMap.put(namespace.getUri(), newModel.getModel());
                }
                break;
            case REMOVED:
                // At the moment we do not unload models - I can see no side effects ....
                // However search is used to check for references to indexed properties or types
                // This will be partially broken anyway due to eventual consistency
                // A model should only be unloaded if there are no data dependencies
                // Should have been on the de-lucene list.
                break;
            }
        }

        HashSet<String> loadedModels = new HashSet<String>();
        for (M2Model model : modelMap.values())
        {
            loadModel(modelMap, loadedModels, model);
        }
        if (modelDiffs.size() > 0)
        {
            afterInitModels();
        }

    }

    public void afterInitModels()
    {
        cmisDictionaryService.afterDictionaryInit();
    }


    protected AlfrescoHttpClient getRepoClient(ClasspathKeyResourceLoader keyResourceLoader)
    {
        // TODO i18n
        KeyStoreParameters keyStoreParameters = new KeyStoreParameters("SSL Key Store", "JCEKS", null, "ssl-keystore-passwords.properties", "ssl.repo.client.keystore");
        KeyStoreParameters trustStoreParameters = new KeyStoreParameters("SSL Trust Store", "JCEKS", null, "ssl-truststore-passwords.properties", "ssl.repo.client.truststore");
 
        SSLEncryptionParameters sslEncryptionParameters = new SSLEncryptionParameters(keyStoreParameters, trustStoreParameters);

    	//MD5HttpClientFactory httpClientFactory = new MD5HttpClientFactory();
//        HttpClientFactory httpClientFactory = new HttpClientFactory(SecureComm);
//
//        invalidKeyEncryptionService = new EncryptionService("127.0.0.1", 8080, keyResourceLoader, keyStoreParameters, encryptionParameters);
//        AlfrescoHttpClient repoClient = httpClientFactory.getAlfrescoHttpClient("127.0.0.1", 8080, invalidKeyEncryptionService);
//        //SecureHttpClient repoClient = new SecureHttpClient(httpClientFactory, "127.0.0.1", 8080, invalidKeyEncryptionService);
//        invalidKeyClient = new SOLRAPIClient(repoClient, model.getDictionaryService(), namespaceDAO);
//
//        keyStoreParameters.setLocation("org/alfresco/solr/client/.keystore");
//        tamperWithEncryptionService = new TamperWithEncryptionService("127.0.0.1", 8080, keyResourceLoader, keyStoreParameters, encryptionParameters);
//        repoClient = httpClientFactory.getAlfrescoHttpClient("127.0.0.1", 8080, tamperWithEncryptionService);
////        repoClient = new SecureHttpClient(httpClientFactory, "127.0.0.1", 8080, tamperWithEncryptionService);
//        tamperWithClient = new SOLRAPIClient(repoClient, model.getDictionaryService(), namespaceDAO);
//        
//        encryptionParameters.setMessageTimeout(30*1000);
//        keyStoreParameters.setLocation("workspace-SpacesStore/conf/.keystore");
//        EncryptionService encryptionService = new EncryptionService("127.0.0.1", 8080, keyResourceLoader, keyStoreParameters, encryptionParameters);
//        repoClient = httpClientFactory.getAlfrescoHttpClient("127.0.0.1", 8080, encryptionService);
        //repoClient = new SecureHttpClient(httpClientFactory, "127.0.0.1", 8080, encryptionService);
//        client = new SOLRAPIClient(repoClient, model.getDictionaryService(), namespaceDAO);

        HttpClientFactory httpClientFactory = new HttpClientFactory(SecureCommsType.getType("https"), sslEncryptionParameters, keyResourceLoader, null, null, "localhost", 8080,
                8443, 40, 40, 0);
        // TODO need to make port configurable depending on secure comms, or just make redirects
        // work
        return httpClientFactory.getRepoClient("localhost", 8443);
    }

    public ClassLoader getResourceClassLoader()
    {

        File f = new File("woof", "alfrescoResources");
        if (f.canRead() && f.isDirectory())
        {

            URL[] urls = new URL[1];

            try
            {
                URL url = f.toURI().normalize().toURL();
                urls[0] = url;
            }
            catch (MalformedURLException e)
            {
                throw new AlfrescoRuntimeException("Failed to add resources to classpath ", e);
            }

            return URLClassLoader.newInstance(urls, this.getClass().getClassLoader());
        }
        else
        {
            return this.getClass().getClassLoader();
        }
    }

    private class ClasspathKeyResourceLoader implements KeyResourceLoader
    {
        @Override
        public InputStream getKeyStore(String location) throws FileNotFoundException
        {
            return getClass().getClassLoader().getResourceAsStream(location);
        }

        @Override
        public Properties loadKeyMetaData(String location) throws IOException
        {
            Properties p = new Properties();
            p.load(getClass().getClassLoader().getResourceAsStream(location));
            return p;
        }
    }

    /**
     * Full testing of ChangeSets, ACLs and Readers
     */
    public void testGetAcls() throws Exception
    {
        AclChangeSets aclChangeSets = null;

        aclChangeSets = client.getAclChangeSets(null, null, null, null, 50);
        assertTrue("Too many results", aclChangeSets.getAclChangeSets().size() <= 50);
        if (aclChangeSets.getAclChangeSets().size() < 2)
        {
            return; // Not enough data
        }
        AclChangeSet aclChangeSetCheck = null;
        AclChangeSet aclChangeSet0 = aclChangeSets.getAclChangeSets().get(0);
        AclChangeSet aclChangeSet1 = aclChangeSets.getAclChangeSets().get(1);
        long id0 = aclChangeSet0.getId();
        long commitTimeMs0 = aclChangeSet0.getCommitTimeMs();
        // Now query for the next ID
        Long nextId = id0 + 1;
        aclChangeSets = client.getAclChangeSets(commitTimeMs0, nextId, null, null, 1);
        assertEquals(1, aclChangeSets.getAclChangeSets().size());
        aclChangeSetCheck = aclChangeSets.getAclChangeSets().get(0);
        assertEquals(aclChangeSet1, aclChangeSetCheck);

        Map<Long, AclChangeSet> aclChangeSetsById = new HashMap<Long, AclChangeSet>();
        for (AclChangeSet aclChangeSet : aclChangeSets.getAclChangeSets())
        {
            aclChangeSetsById.put(aclChangeSet.getId(), aclChangeSet);
        }

        Set<Long> aclIdUniqueCheck = new HashSet<Long>(1000);
        // Now do a large walk-through of the ACLs
        Long minAclChangeSetId = null;
        Long fromCommitTimeMs = null;
        for (int i = 0; i < 100; i++)
        {
            aclChangeSets = client.getAclChangeSets(fromCommitTimeMs, minAclChangeSetId, null, null, 10);
            if (aclChangeSets.getAclChangeSets().size() == 0)
            {
                break;
            }
            // Now repeat for the ACLs, keeping track of the last ChangeSet
            Long nextAclId = null;
            while (true)
            {
                List<Acl> acls = client.getAcls(aclChangeSets.getAclChangeSets(), nextAclId, 512);
                if (acls.size() == 0)
                {
                    break; // Run out of ACLs
                }
                Set<Long> aclIds = new HashSet<Long>(1000);
                for (Acl acl : acls)
                {
                    long aclId = acl.getId();
                    aclIds.add(aclId);
                    if (!aclIdUniqueCheck.add(aclId))
                    {
                        // ignore duplicates for lazy/eager shared acl creation
                        //fail("ACL already processed: " + aclId);
                    }
                    // Check that we are ascending
                    if (nextAclId != null)
                    {
                        assertTrue("ACL IDs must be ascending: " + aclId, nextAclId.longValue() <= aclId);
                    }
                    nextAclId = aclId + 1;
                }
                // Now get the readers for these ACLs
                List<AclReaders> aclsReaders = client.getAclReaders(acls);
                // Check that the ACL ids are all covered
                for (AclReaders aclReaders : aclsReaders)
                {
                    Long aclId = aclReaders.getId();
                    aclIds.remove(aclId);
                }
                assertTrue("Some ACL IDs were not covered: " + aclIds, aclIds.size() == 0);
            }
            // March in time
            AclChangeSet lastAclChangeSet = aclChangeSets.getAclChangeSets().get(aclChangeSets.getAclChangeSets().size() - 1);
            fromCommitTimeMs = lastAclChangeSet.getCommitTimeMs();
            minAclChangeSetId = lastAclChangeSet.getId() + 1;
        }
    }

    public void testGetTransactions() throws Exception
    {
        // get transactions starting from txn id 1298288417234l
        Transactions transactions = client.getTransactions(1298288417234l, null, null, null, 5);

        // get transactions starting from transaction 426
        transactions = client.getTransactions(null, Long.valueOf(1), null, null, 5);
        List<Long> transactionIds = new ArrayList<Long>(transactions.getTransactions().size());
        for (Transaction info : transactions.getTransactions())
        {
            logger.debug(info);
            transactionIds.add(Long.valueOf(info.getId()));
        }

        // get the first 3 nodes in those transactions
        GetNodesParameters params = new GetNodesParameters();
        params.setTransactionIds(transactionIds);
        List<Node> nodes = client.getNodes(params, 5);
        for (Node info : nodes)
        {
            logger.debug(info);
        }

        // get the next 3 nodes in those transactions i.e. starting from the last node id (inclusive)
        params = new GetNodesParameters();
        params.setTransactionIds(transactionIds);
        params.setFromNodeId(nodes.get(nodes.size() - 1).getId());
        nodes = client.getNodes(params, 3);
        List<Long> nodeIds = new ArrayList<Long>(nodes.size());
        for (Node info : nodes)
        {
            logger.debug(info);
            nodeIds.add(info.getId());
        }

        NodeMetaDataParameters metaParams = new NodeMetaDataParameters();
        metaParams.setNodeIds(nodeIds);
        List<NodeMetaData> metadata = client.getNodesMetaData(metaParams, 3);
        for (NodeMetaData info : metadata)
        {
            logger.debug(info);
        }
    }

    public void testMetaData() throws AuthenticationException, IOException, JSONException
    {
        NodeMetaDataParameters metaParams = new NodeMetaDataParameters();
        List<Long> nodeIds = new ArrayList<Long>(1);
        nodeIds.add(1l);
        metaParams.setNodeIds(nodeIds);
        List<NodeMetaData> metadata = client.getNodesMetaData(metaParams, 3);
        for (NodeMetaData info : metadata)
        {
            logger.debug(info);
        }

        metaParams = new NodeMetaDataParameters();
        nodeIds = new ArrayList<Long>(1);
        nodeIds.add(9l);
        metaParams.setNodeIds(nodeIds);
        metadata = client.getNodesMetaData(metaParams, 3);
        for (NodeMetaData info : metadata)
        {
            logger.debug(info);
        }

        metaParams = new NodeMetaDataParameters();
        nodeIds = new ArrayList<Long>(1);
        nodeIds.add(19l);
        metaParams.setNodeIds(nodeIds);
        metadata = client.getNodesMetaData(metaParams, 3);
        for (NodeMetaData info : metadata)
        {
            logger.debug(info);
        }

        // individual tag/category
        // TODO check why the category path has a null QName
        metaParams = new NodeMetaDataParameters();
        nodeIds = new ArrayList<Long>(1);
        nodeIds.add(49437l);
        metaParams.setNodeIds(nodeIds);
        metadata = client.getNodesMetaData(metaParams, 3);
        for (NodeMetaData info : metadata)
        {
            logger.debug(info);
        }

        // content with tags
        metaParams = new NodeMetaDataParameters();
        nodeIds = new ArrayList<Long>(1);
        nodeIds.add(49431l);
        metaParams.setNodeIds(nodeIds);
        metadata = client.getNodesMetaData(metaParams, 3);
        for (NodeMetaData info : metadata)
        {
            logger.debug(info);
            MultiPropertyValue multi = (MultiPropertyValue) info.getProperties().get(QName.createQName("{http://www.alfresco.org/model/content/1.0}taggable"));
            if(multi != null)
            {
                for (PropertyValue propValue : multi.getValues())
                {
                    logger.debug("multi property values = " + propValue);
                }
            }
        }

        // content with null property values for author
        metaParams = new NodeMetaDataParameters();
        nodeIds = new ArrayList<Long>(1);
        nodeIds.add(117630l);
        metaParams.setNodeIds(nodeIds);
        metadata = client.getNodesMetaData(metaParams, 3);
        for (NodeMetaData info : metadata)
        {
            logger.debug(info);
        }

        // content with accented characters in title properties
        metaParams = new NodeMetaDataParameters();
        nodeIds = new ArrayList<Long>(1);
        nodeIds.add(117678l);
        metaParams.setNodeIds(nodeIds);
        metadata = client.getNodesMetaData(metaParams, 3);
        for (NodeMetaData info : metadata)
        {
            logger.debug(info);
        }
    }

    public void testGetModel() throws AuthenticationException, IOException, JSONException
    {
        AlfrescoModel alfModel = client.getModel(CORENAME,QName.createQName("http://www.alfresco.org/model/content/1.0", "contentmodel"));
        M2Model model = alfModel.getModel();
        assertNotNull(model);
        assertEquals("Returned model has incorrect name", "cm:contentmodel", model.getName());
        assertNotNull(alfModel.getChecksum());
    }

    public void testGetModelDiffs() throws AuthenticationException, IOException, JSONException
    {
        List<AlfrescoModelDiff> diffs = client.getModelsDiff(CORENAME,Collections.<AlfrescoModel> emptyList());
        assertTrue(diffs.size() > 0);
    }

//    public void testMAC() throws IOException, JSONException
//    {
//        // dodyClient has a secret key that is not the same as the repository's. This
//        // should fail with a 401
//        try
//        {
//            Transactions transactions = invalidKeyClient.getTransactions(1298288417234l, null, null, null, 5);
//        }
//        catch (AuthenticationException e)
//        {
//            assertEquals("Should have caught unathorised request", e.getMethod().getStatusCode(), HttpStatus.SC_UNAUTHORIZED);
//        }
//
//        try
//        {
//            tamperWithEncryptionService.setOverrideTimestamp(true);
//            Transactions transactions = tamperWithClient.getTransactions(1298288417234l, null, null, null, 5);
//        }
//        catch (AuthenticationException e)
//        {
//            assertEquals("Should have caught unathorised request", e.getMethod().getStatusCode(), HttpStatus.SC_UNAUTHORIZED);
//        }
//        finally
//        {
//            tamperWithEncryptionService.setOverrideTimestamp(false);
//        }
//
//        try
//        {
//            tamperWithEncryptionService.setOverrideMAC(true);
//            Transactions transactions = tamperWithClient.getTransactions(1298288417234l, null, null, null, 5);
//        }
//        catch (AuthenticationException e)
//        {
//            assertEquals("Should have caught unathorised request", e.getMethod().getStatusCode(), HttpStatus.SC_UNAUTHORIZED);
//        }
//        finally
//        {
//            tamperWithEncryptionService.setOverrideMAC(false);
//        }
//    }

    private void outputTextContent(SOLRAPIClient.GetTextContentResponse response) throws IOException
    {
        InputStream in = response.getContent();
        if (in != null)
        {
            logger.debug("Text content:");

            BufferedReader reader = new BufferedReader(new InputStreamReader(in, "UTF-8"));
            String line = null;
            while ((line = reader.readLine()) != null)
            {
                logger.debug(line);
            }
        }
    }

    /**
     * Overrides request encryption to create dodgy MAC and timestamp on requests
     */
    private static class TestEncryptionUtils extends DefaultEncryptionUtils
    {
        private boolean overrideMAC = false;

        private boolean overrideTimestamp = false;

        public void setOverrideMAC(boolean overrideMAC)
        {
            this.overrideMAC = overrideMAC;
        }

        public void setOverrideTimestamp(boolean overrideTimestamp)
        {
            this.overrideTimestamp = overrideTimestamp;
        }

        @Override
        public void setRequestAuthentication(HttpMethod method, byte[] message) throws IOException
        {
            if (method instanceof PostMethod)
            {
                // encrypt body
                Pair<byte[], AlgorithmParameters> encrypted = encryptor.encrypt(KeyProvider.ALIAS_SOLR, null, message);
                setRequestAlgorithmParameters(method, encrypted.getSecond());

                ((PostMethod) method).setRequestEntity(new ByteArrayRequestEntity(encrypted.getFirst(), "application/octet-stream"));
            }

            long requestTimestamp = System.currentTimeMillis();

            // add MAC header
            byte[] mac = macUtils.generateMAC(KeyProvider.ALIAS_SOLR, new MACInput(message, requestTimestamp, getLocalIPAddress()));

            if (logger.isDebugEnabled())
            {
                logger.debug("Setting MAC " + mac + " on HTTP request " + method.getPath());
                logger.debug("Setting timestamp " + requestTimestamp + " on HTTP request " + method.getPath());
            }

            if (overrideMAC)
            {
                mac[0] += (byte) 1;
            }
            setRequestMac(method, mac);

            if (overrideTimestamp)
            {
                requestTimestamp += 60000;
            }
            // prevent replays
            setRequestTimestamp(method, requestTimestamp);
        }
    }
}
