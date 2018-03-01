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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;

import org.alfresco.httpclient.AuthenticationException;
import org.alfresco.repo.dictionary.M2Model;
import org.alfresco.repo.dictionary.NamespaceDAO;
import org.alfresco.service.namespace.QName;
import org.alfresco.solr.InformationServer;
import org.alfresco.solr.client.AlfrescoModel;
import org.alfresco.solr.client.AlfrescoModelDiff;
import org.alfresco.solr.client.AlfrescoModelDiff.TYPE;
import org.alfresco.solr.client.SOLRAPIClient;
import org.json.JSONException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ModelTrackerTest
{
    private ModelTracker modelTracker;

    @Mock
    private SOLRAPIClient repositoryClient;
    private String coreName = "theCoreName";
    @Mock
    private InformationServer srv;
    @Mock
    private Properties props;
    @Mock
    private TrackerStats trackerStats;

    private static File alfrescoModelDir;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception
    {
        alfrescoModelDir = new File("target/testalfrescoModels");
        alfrescoModelDir.mkdir();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception
    {
        try {
            File[] listFiles = alfrescoModelDir.listFiles();
            for (File file : listFiles) 
            {
                file.delete();
            }
            alfrescoModelDir.delete();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    @Before
    public void setUp() throws Exception
    {
        when(props.getProperty("alfresco.stores", "workspace://SpacesStore")).thenReturn("workspace://SpacesStore");
        when(props.getProperty("alfresco.batch.count", "5000")).thenReturn("5000");
        when(props.getProperty("alfresco.maxLiveSearchers", "2")).thenReturn("2");
        when(props.getProperty("enable.slave", "false")).thenReturn("false");
        when(props.getProperty("enable.master", "true")).thenReturn("true");
        when(props.getProperty("shard.count", "1")).thenReturn("1");
        when(props.getProperty("shard.instance", "0")).thenReturn("0");
        when(this.srv.getTrackerStats()).thenReturn(trackerStats);
        System.setProperty("solr.model.dir", alfrescoModelDir.getAbsolutePath());
        this.modelTracker = new ModelTracker(null, props, repositoryClient, coreName, srv);
    }

    @After
    public void tearDown() throws Exception
    {
    }

    @Test
    public void testDoTrack() throws AuthenticationException, IOException, JSONException
    {
        ModelTracker spiedModelTracker = spy(this.modelTracker);
        spiedModelTracker.doTrack();

        verify(this.srv).getRegisteredSearcherCount();
        verify(spiedModelTracker).trackModels(false);
    }

    @Test
    public void testTrackModels() throws AuthenticationException, IOException, JSONException
    {
        final String name = setUpTestTrackModels();

        this.modelTracker.trackModels(false);

        verifyTestTrackModels(name);
    }

    private void verifyTestTrackModels(final String name)
    {
        verify(this.srv).afterInitModels();

        assertTrue(alfrescoModelDir.isDirectory());

        File[] modelRepresentations = alfrescoModelDir.listFiles(new FileFilter()
        {
            @Override
            public boolean accept(File pathname)
            {
                return pathname.getName().startsWith(name);
            }
        });
        assertEquals(1, modelRepresentations.length);
    }

    @SuppressWarnings("unchecked")
    private String setUpTestTrackModels() throws AuthenticationException, IOException, JSONException
    {
        QName modelName = QName.createQName("qname");
        TYPE type = TYPE.CHANGED;
        Long oldChecksum = new Long(0);
        Long newChecksum = new Long(1);
        AlfrescoModelDiff diff = new AlfrescoModelDiff(modelName, type, oldChecksum, newChecksum);
        List<AlfrescoModelDiff> modelDiffs = new ArrayList<>();
        modelDiffs.add(diff);
        when(this.repositoryClient.getModelsDiff(any(String.class),any(List.class))).thenReturn(modelDiffs);

        final String name = "a model name";
        M2Model model = M2Model.createModel(name);
        M2Model spiedModel = spy(model);
        model.createNamespace("uri", "prefix");
        AlfrescoModel alfrescoModel = new AlfrescoModel(spiedModel, newChecksum);
        when(this.repositoryClient.getModel(any(String.class),eq(modelName))).thenReturn(alfrescoModel);

        NamespaceDAO namespaceDao = mock(NamespaceDAO.class);
        Collection<String> values = new ArrayList<>();
        values.add("prefix");
        when(namespaceDao.getPrefixes(anyString())).thenReturn(values);
        when(this.srv.getNamespaceDAO()).thenReturn(namespaceDao);
        when(this.srv.getM2Model(modelName)).thenReturn(spiedModel);
        when(this.srv.putModel(spiedModel)).thenReturn(true);
        return name;
    }

    @Test
    public void testEnsureFirstModelSync() throws AuthenticationException, IOException, JSONException
    {
        final String name = setUpTestTrackModels();

        this.modelTracker.ensureFirstModelSync();

        verifyTestTrackModels(name);
    }
}
