/*
 * Copyright (C) 2014 Alfresco Software Limited.
 *
 * This file is part of Alfresco
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
 */
package org.alfresco.solr.tracker;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.httpclient.AuthenticationException;
import org.alfresco.repo.dictionary.M2Model;
import org.alfresco.repo.dictionary.M2Namespace;
import org.alfresco.service.cmr.dictionary.ModelDefinition.XMLBindingType;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.namespace.QName;
import org.alfresco.solr.AlfrescoSolrDataModel;
import org.alfresco.solr.InformationServer;
import org.alfresco.solr.client.AlfrescoModel;
import org.alfresco.solr.client.AlfrescoModelDiff;
import org.alfresco.solr.client.SOLRAPIClient;
import org.alfresco.solr.config.ConfigUtil;
import org.apache.solr.core.SolrResourceLoader;
import org.json.JSONException;

/**
 * @startuml
 * title ModelTracker
 * participant Scheduler
 * participant ModelTracker
 * participant SOLRAPIClient
 * == Initialisation ==
 * [-> ModelTracker: <<create>>
 * activate ModelTracker
 * ModelTracker -> ModelTracker : locate create persisted models 
 * ModelTracker -> ModelTracker : load persisted models 
 * deactivate ModelTracker
 * == Tracking ==
 * Scheduler -> ModelTracker: Run
 * activate ModelTracker
 * ModelTracker -> SOLRAPIClient: getModelChanges(Models + CRCs)
 * activate SOLRAPIClient
 * SOLRAPIClient -> ModelTracker: changedModels
 * deactivate SOLRAPIClient
 * loop changed models
 *    ModelTracker -> SOLRAPIClient: getModel
 *    activate SOLRAPIClient
 *    SOLRAPIClient -> ModelTracker: Model XML
 *    deactivate SOLRAPIClient
 * end
 * ModelTracker -> ModelTracker: update local dictionary
 * ModelTracker -> ModelTracker: update cached models
 * ModelTracker -> ModelTracker: intialise model dependancies
 * ModelTracker -> Scheduler
 * deactivate ModelTracker
 * @enduml
 */
public class ModelTracker extends AbstractTracker implements Tracker
{

    private final Set<StoreRef> indexedStores = new HashSet<>();
    private final Set<StoreRef> ignoredStores = new HashSet<>();
    private final Set<String> indexedTenants = new HashSet<>();
    private final Set<String> ignoredTenants = new HashSet<>();
    private final Set<QName> indexedDataTypes = new HashSet<>();
    private final Set<QName> ignoredDataTypes = new HashSet<>();
    private final Set<QName> indexedTypes = new HashSet<>();
    private final Set<QName> ignoredTypes = new HashSet<>();
    private final Set<QName> indexedAspects = new HashSet<>();
    private final Set<QName> ignoredAspects = new HashSet<>();
    private final Set<String> indexedFields = new HashSet<>();
    private final Set<String> ignoredFields = new HashSet<>();

    private ReentrantReadWriteLock modelLock = new ReentrantReadWriteLock();
    private volatile boolean hasModels = false;
    private File alfrescoModelDir;

    public ModelTracker(String solrHome, Properties p, SOLRAPIClient client, String coreName,
                InformationServer informationServer)
    {
        super(p, client, coreName, informationServer, Tracker.Type.MODEL);
        String normalSolrHome = SolrResourceLoader.normalizeDir(solrHome);
        alfrescoModelDir = new File(ConfigUtil.locateProperty("solr.model.dir", normalSolrHome+"alfrescoModels"));
        logger.info("Alfresco Model dir " + alfrescoModelDir);
        if (!alfrescoModelDir.exists())
        {
            alfrescoModelDir.mkdir();
        }

        loadPersistedModels();
    }

    @Override
    public boolean hasMaintenance()
    {
        return false;
    }

    @Override
    public void maintenance()
    {
        // Nothing to be done here
    }

    /**
     *
     */
    private void loadPersistedModels()
    {
        HashMap<String, M2Model> modelMap = new HashMap<>();
        if (alfrescoModelDir.exists() && alfrescoModelDir.isDirectory())
        {
            // A filter for XML files
            FileFilter filter = pathname -> pathname.isFile() && pathname.getName().endsWith(".xml");
            // List XML files
            File[] files = alfrescoModelDir.listFiles(filter);
            if (files != null)
            {
                for (File file : files)
                {
                    InputStream modelStream = null;
                    M2Model model;
                    try
                    {
                        modelStream = new FileInputStream(file);
                        model = M2Model.createModel(modelStream);
                    }
                    catch (IOException e)
                    {
                        throw new AlfrescoRuntimeException("File not found: " + file, e);
                    }
                    finally
                    {
                        if (modelStream != null)
                        {
                            try { modelStream.close(); } catch (Exception e) {}
                        }
                    }
                    // Model successfully loaded
                    for (M2Namespace namespace : model.getNamespaces())
                    {
                        modelMap.put(namespace.getUri(), model);
                    }
                }
            }
        }
        // Load the models ensuring that they are loaded in the correct order
        HashSet<String> loadedModels = new HashSet<String>();
        for (M2Model model : modelMap.values())
        {
            loadModel(modelMap, loadedModels, model);
        }

        if(modelMap.size() > 0)
        {
            AlfrescoSolrDataModel.getInstance().afterInitModels();
        }
    }

    /**
     * Default constructor, for testing.
     */
    ModelTracker()
    {
        super(Tracker.Type.MODEL);
    }

    @Override
    protected void doTrack(String iterationId) throws AuthenticationException, IOException, JSONException
    {
        // Is the InformationServer ready to update
        int registeredSearcherCount = this.infoSrv.getRegisteredSearcherCount();
        if (registeredSearcherCount >= getMaxLiveSearchers())
        {
            logger.info(".... skipping tracking registered searcher count = " + registeredSearcherCount);
            return;
        }

        checkShutdown();
        trackModels(false);
    }

    public void trackModels(boolean onlyFirstTime) throws AuthenticationException, IOException, JSONException
    {
        boolean requiresWriteLock;
        modelLock.readLock().lock();
        try
        {
            if (hasModels)
            {
                if (onlyFirstTime)
                {
                    return;
                }
                else
                {
                    requiresWriteLock = false;
                }
            }
            else
            {
                requiresWriteLock = true;
            }
        }
        finally
        {
            modelLock.readLock().unlock();
        }

        if (requiresWriteLock)
        {
            modelLock.writeLock().lock();
            try
            {
                if (hasModels)
                {
                    if (onlyFirstTime) { return; }
                }

                trackModelsImpl();
                if (onlyFirstTime)
                {
                    infoSrv.initSkippingDescendantDocs();
                }
                hasModels = true;
            }
            finally
            {
                modelLock.writeLock().unlock();
            }
        }
        else
        {
            trackModelsImpl();
        }
    }

    public void ensureFirstModelSync()
    {
        try
        {
            trackModels(true);
        }
        catch (Throwable t)
        {
            logger.error("Model tracking failed for core: "+ coreName, t);
        }

    }

    /**
     * Tracks models. Reflects changes and updates on disk copy
     */
    private void trackModelsImpl() throws AuthenticationException, IOException, JSONException
    {
        long start = System.nanoTime();

        List<AlfrescoModelDiff> modelDiffs = client.getModelsDiff(coreName, this.infoSrv.getAlfrescoModels());
        Map<String, M2Model> modelMap = new HashMap<>();

        for (AlfrescoModelDiff modelDiff : modelDiffs)
        {
            switch (modelDiff.getType())
            {
                case CHANGED:
                case NEW:
                    AlfrescoModel model = client.getModel(coreName, modelDiff.getModelName());
                    for (M2Namespace namespace : model.getModel().getNamespaces())
                    {
                        modelMap.put(namespace.getUri(), model.getModel());
                    }
                    break;
                case REMOVED:
                	// We now remove models as we see them - MNT-17627
                	// Models have to be deleted from disk before we remove them from memory
                	// We need to know the prefix for the uri to delete them
                    break;
            }
        }

        HashSet<String> loadedModels = new HashSet<>();
        for (M2Model model : modelMap.values())
        {
            loadModel(modelMap, loadedModels, model);
        }
        if (loadedModels.size() > 0)
        {
            this.infoSrv.afterInitModels();
        }

        for (AlfrescoModelDiff modelDiff : modelDiffs)
        {
            switch (modelDiff.getType())
            {
                case CHANGED:
                    removeMatchingModels(alfrescoModelDir, modelDiff.getModelName());
                    M2Model changedModel = this.infoSrv.getM2Model(modelDiff.getModelName());
                    File changedFile = new File(alfrescoModelDir, getModelFileName(changedModel));
                    FileOutputStream cos = new FileOutputStream(changedFile);
                    changedModel.toXML(cos);
                    cos.flush();
                    cos.close();
                    break;
                case NEW:
                    M2Model newModel = this.infoSrv.getM2Model(modelDiff.getModelName());
                    // add on file
                    File newFile = new File(alfrescoModelDir, getModelFileName(newModel));
                    FileOutputStream nos = new FileOutputStream(newFile);
                    newModel.toXML(nos);
                    nos.flush();
                    nos.close();
                    break;
                case REMOVED:
                	// This will remove the model from the dictionary on completion
                	try
                	{
                        removeMatchingModels(alfrescoModelDir, modelDiff.getModelName());
                	}
                	finally
                	{
                		AlfrescoSolrDataModel.getInstance().removeModel(modelDiff.getModelName());
                	}
                    break;
            }
        }

        long end = System.nanoTime();

        trackerStats.addModelTime(end - start);

        if (runPostModelLoadInit)
        {
            for (Object key : props.keySet())
            {
                String stringKey = (String) key;
                if (stringKey.startsWith("alfresco.index.store"))
                {
                    StoreRef store = new StoreRef(props.getProperty(stringKey));
                    indexedStores.add(store);
                }
                if (stringKey.startsWith("alfresco.ignore.store"))
                {
                    StoreRef store = new StoreRef(props.getProperty(stringKey));
                    ignoredStores.add(store);
                }
                if (stringKey.startsWith("alfresco.index.tenant"))
                {
                    indexedTenants.add(props.getProperty(stringKey));
                }
                if (stringKey.startsWith("alfresco.ignore.tenant"))
                {
                    ignoredTenants.add(props.getProperty(stringKey));
                }
                if (stringKey.startsWith("alfresco.index.datatype"))
                {
                    QName qname = expandQName(props.getProperty(stringKey));
                    indexedDataTypes.add(qname);
                }
                if (stringKey.startsWith("alfresco.ignore.datatype"))
                {
                    QName qname = expandQName(props.getProperty(stringKey));
                    ignoredDataTypes.add(qname);
                }
                if (stringKey.startsWith("alfresco.index.type"))
                {
                    QName qname = expandQName(props.getProperty(stringKey));
                    indexedTypes.add(qname);
                }
                if (stringKey.startsWith("alfresco.ignore.type"))
                {
                    QName qname = expandQName(props.getProperty(stringKey));
                    ignoredTypes.add(qname);
                }
                if (stringKey.startsWith("alfresco.index.aspect"))
                {
                    QName qname = expandQName(props.getProperty(stringKey));
                    indexedAspects.add(qname);
                }
                if (stringKey.startsWith("alfresco.ignore.aspect"))
                {
                    QName qname = expandQName(props.getProperty(stringKey));
                    ignoredAspects.add(qname);
                }
                if (stringKey.startsWith("alfresco.index.field"))
                {
                    String name = expandName(props.getProperty(stringKey));
                    indexedFields.add(name);
                }
                if (stringKey.startsWith("alfresco.ignore.field"))
                {
                    String name = expandName(props.getProperty(stringKey));
                    ignoredFields.add(name);
                }
            }
            runPostModelLoadInit = false;
        }
    }

    private QName expandQName(String qName)
    {
        String expandedQName = qName;
        if (qName.startsWith("@"))
        {
            return expandQName(qName.substring(1));
        }
        else if (qName.startsWith("{"))
        {
            expandedQName = expandQNameImpl(qName);
        }
        else if (qName.contains(":"))
        {
            expandedQName = expandQNameImpl(qName);
        }

        return QName.createQName(expandedQName);

    }

    private String expandQNameImpl(String q)
    {
        String eq = q;
        // Check for any prefixes and expand to the full uri
        if (q.charAt(0) != '{')
        {
            int colonPosition = q.indexOf(':');
            if (colonPosition == -1)
            {
                // use the default namespace
                eq = "{" + this.infoSrv.getNamespaceDAO().getNamespaceURI("") + "}" + q;
            }
            else
            {
                // find the prefix
                eq = "{" + this.infoSrv.getNamespaceDAO().getNamespaceURI(q.substring(0, colonPosition)) + "}"
                            + q.substring(colonPosition + 1);
            }
        }
        return eq;
    }

    private String expandName(String qName)
    {
        String expandedQName = qName;
        if (qName.startsWith("@"))
        {
            return expandName(qName.substring(1));
        }
        else if (qName.startsWith("{"))
        {
            expandedQName = expandQNameImpl(qName);
        }
        else if (qName.contains(":"))
        {
            expandedQName = expandQNameImpl(qName);
        }

        return expandedQName;
    }

    private void removeMatchingModels(File alfrescoModelDir, QName modelName)
    {
        final String prefix = modelName.toPrefixString(this.infoSrv.getNamespaceDAO()).replace(":", ".") + ".";
        final String postFix = ".xml";

        File[] toDelete = alfrescoModelDir.listFiles(pathname -> {
            if (pathname.isDirectory()) { return false; }
            String name = pathname.getName();
            if (!name.endsWith(postFix)) { return false; }
            if (!name.startsWith(prefix)) { return false; }
            // check is number between
            String checksum = name.substring(prefix.length(), name.length() - postFix.length());
            try
            {
                Long.parseLong(checksum);
                return true;
            }
            catch (NumberFormatException nfe)
            {
                return false;
            }
        });

        if (toDelete != null)
        {
            for (File file : toDelete)
            {
                file.delete();
            }
        }
    }

    private void loadModel(Map<String, M2Model> modelMap, HashSet<String> loadedModels, M2Model model)
    {
        String modelName = model.getName();
        if (!loadedModels.contains(modelName))
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

            if (this.infoSrv.putModel(model))
            {
                loadedModels.add(modelName);
            }
            logger.info("Loading model " + model.getName());
        }
    }

    private String getModelFileName(M2Model model)
    {
        return model.getName().replace(":", ".") + "." + model.getChecksum(XMLBindingType.DEFAULT) + ".xml";
    }

    public boolean hasModels()
    {
        return this.hasModels;
    }
}
