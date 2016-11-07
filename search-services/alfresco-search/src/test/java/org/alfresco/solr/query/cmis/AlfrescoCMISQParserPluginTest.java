/*
 * Copyright (C) 2005-2014 Alfresco Software Limited.
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

package org.alfresco.solr.query.cmis;

import org.alfresco.repo.search.adaptor.lucene.QueryConstants;
import org.apache.lucene.util.LuceneTestCase;
import org.apache.solr.SolrTestCaseJ4;
import org.junit.Test;


@LuceneTestCase.SuppressCodecs({"Appending","Lucene3x","Lucene40","Lucene41","Lucene42","Lucene43", "Lucene44", "Lucene45","Lucene46","Lucene47","Lucene48","Lucene49"})
@SolrTestCaseJ4.SuppressSSL
public class AlfrescoCMISQParserPluginTest extends LoadCMISData implements QueryConstants 
{
    @Test
    public void cmisBasic() throws Exception
    {
        assertQ(qurySolr("SELECT * from cmis:folder"),
                expectedDocCount(11));
        assertQ(qurySolr("SELECT * from cmis:document"),
                expectedDocCount(11));
    }
    
    @Test
    public void testCMISParentID()
    {
        assertQ(qurySolr("SELECT cmis:parentId FROM cmis:folder WHERE cmis:parentId =  '"
                        + testCMISBaseFolderNodeRef + "'"),
                expectedDocCount(4));

        assertQ(qurySolr("SELECT cmis:parentId FROM cmis:folder WHERE cmis:parentId <> '"
                        + testCMISBaseFolderNodeRef + "'"),
                expectedDocCount(7));

        assertQ(qurySolr("SELECT cmis:parentId FROM cmis:folder WHERE cmis:parentId IN     ('"
                        + testCMISBaseFolderNodeRef + "')"),
                expectedDocCount(4));

        assertQ(qurySolr("SELECT cmis:parentId FROM cmis:folder WHERE cmis:parentId NOT IN ('"
                        + testCMISBaseFolderNodeRef + "')"),
                expectedDocCount(7));

        assertQ(qurySolr("SELECT cmis:parentId FROM cmis:folder WHERE cmis:parentId IS NOT NULL"),
                expectedDocCount(11));

        assertQ(qurySolr("SELECT cmis:parentId FROM cmis:folder WHERE cmis:parentId IS NULL"),
                expectedDocCount(0));
    }
    @Test
    public void checkCmisContentStreamFileName()
    {
        assertQ(qurySolr("SELECT cmis:contentStreamFileName FROM cmis:document WHERE cmis:contentStreamFileName =  'Alfresco Tutorial'"),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT cmis:contentStreamFileName FROM cmis:document WHERE cmis:contentStreamFileName =  'AA%'"),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT cmis:contentStreamFileName FROM cmis:document WHERE cmis:contentStreamFileName =  'BB_'"),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT cmis:contentStreamFileName FROM cmis:document WHERE cmis:contentStreamFileName =  'CC\\\\'"),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT cmis:contentStreamFileName FROM cmis:document WHERE cmis:contentStreamFileName =  'DD\\''"),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT cmis:contentStreamFileName FROM cmis:document WHERE cmis:contentStreamFileName =  'EE.aa'"),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT cmis:contentStreamFileName FROM cmis:document WHERE cmis:contentStreamFileName =  'FF.EE'"),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT cmis:contentStreamFileName FROM cmis:document WHERE cmis:contentStreamFileName =  'GG*GG'"),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT cmis:contentStreamFileName FROM cmis:document WHERE cmis:contentStreamFileName =  'HH?HH'"),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT cmis:contentStreamFileName FROM cmis:document WHERE cmis:contentStreamFileName =  'aa'"),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT cmis:contentStreamFileName FROM cmis:document WHERE cmis:contentStreamFileName =  'Alfresco Tutorial'"),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT cmis:contentStreamFileName FROM cmis:document WHERE cmis:contentStreamFileName <> 'Alfresco Tutorial'"),
                expectedDocCount(10));

        assertQ(qurySolr("SELECT cmis:contentStreamFileName FROM cmis:document WHERE cmis:contentStreamFileName <  'Alfresco Tutorial'"),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT cmis:contentStreamFileName FROM cmis:document WHERE cmis:contentStreamFileName <= 'Alfresco Tutorial'"),
                expectedDocCount(2));

        assertQ(qurySolr("SELECT cmis:contentStreamFileName FROM cmis:document WHERE cmis:contentStreamFileName >  'Alfresco Tutorial'"),
                expectedDocCount(9));

        assertQ(qurySolr("SELECT cmis:contentStreamFileName FROM cmis:document WHERE cmis:contentStreamFileName >= 'Alfresco Tutorial'"),
                expectedDocCount(10));

        assertQ(qurySolr("SELECT cmis:contentStreamFileName FROM cmis:document WHERE cmis:contentStreamFileName IN     ('Alfresco Tutorial')"),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT cmis:contentStreamFileName FROM cmis:document WHERE cmis:contentStreamFileName NOT IN ('Alfresco Tutorial')"),
                expectedDocCount(10));

        assertQ(qurySolr("SELECT cmis:contentStreamFileName FROM cmis:document WHERE cmis:contentStreamFileName     LIKE 'Alfresco Tutorial'"),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT cmis:contentStreamFileName FROM cmis:document WHERE cmis:contentStreamFileName NOT LIKE 'Alfresco Tutorial'"),
                expectedDocCount(10));

        assertQ(qurySolr("SELECT cmis:contentStreamFileName FROM cmis:document WHERE cmis:contentStreamFileName IS NOT NULL"),
                expectedDocCount(11));

        assertQ(qurySolr("SELECT cmis:contentStreamFileName FROM cmis:document WHERE cmis:contentStreamFileName IS     NULL"),
                expectedDocCount(0));
    }
    @Test
    public void checkCmisContentStreamMimeType()
    {
        assertQ(qurySolr("SELECT cmis:contentStreamMimeType FROM cmis:document WHERE cmis:contentStreamMimeType =  'text/plain'"),
                expectedDocCount(11));

        assertQ(qurySolr("SELECT cmis:contentStreamMimeType FROM cmis:document WHERE cmis:contentStreamMimeType <> 'text/plain'"),
                expectedDocCount(0));

        assertQ(qurySolr("SELECT cmis:contentStreamMimeType FROM cmis:document WHERE cmis:contentStreamMimeType <  'text/plain'"),
                expectedDocCount(0));

        assertQ(qurySolr("SELECT cmis:contentStreamMimeType FROM cmis:document WHERE cmis:contentStreamMimeType <= 'text/plain'"),
                expectedDocCount(11));

        assertQ(qurySolr("SELECT cmis:contentStreamMimeType FROM cmis:document WHERE cmis:contentStreamMimeType >  'text/plain'"),
                expectedDocCount(0));

        assertQ(qurySolr("SELECT cmis:contentStreamMimeType FROM cmis:document WHERE cmis:contentStreamMimeType >= 'text/plain'"),
                expectedDocCount(11));

        assertQ(qurySolr("SELECT cmis:contentStreamMimeType FROM cmis:document WHERE cmis:contentStreamMimeType IN     ('text/plain')"),
                expectedDocCount(11));

        assertQ(qurySolr("SELECT cmis:contentStreamMimeType FROM cmis:document WHERE cmis:contentStreamMimeType NOT IN ('text/plain')"),
                expectedDocCount(0));

        assertQ(qurySolr("SELECT cmis:contentStreamMimeType FROM cmis:document WHERE cmis:contentStreamMimeType     LIKE 'text/plain'"),
                expectedDocCount(11));

        assertQ(qurySolr("SELECT cmis:contentStreamMimeType FROM cmis:document WHERE cmis:contentStreamMimeType NOT LIKE 'text/plain'"),
                expectedDocCount(0));

        assertQ(qurySolr("SELECT cmis:contentStreamMimeType FROM cmis:document WHERE cmis:contentStreamMimeType IS NOT NULL"),
                expectedDocCount(11));

        assertQ(qurySolr("SELECT cmis:contentStreamMimeType FROM cmis:document WHERE cmis:contentStreamMimeType IS     NULL"),
                expectedDocCount(0));
    }
    @Test
    public void checkCmisContentStreamLength()
    {
        assertQ(qurySolr("SELECT cmis:contentStreamLength FROM cmis:document WHERE cmis:contentStreamLength =  750"),
                expectedDocCount(0));

        assertQ(qurySolr("SELECT cmis:contentStreamLength FROM cmis:document WHERE cmis:contentStreamLength <> 750"),
                expectedDocCount(11));

        assertQ(qurySolr("SELECT cmis:contentStreamLength FROM cmis:document WHERE cmis:contentStreamLength <  750"),
                expectedDocCount(11));

        assertQ(qurySolr("SELECT cmis:contentStreamLength FROM cmis:document WHERE cmis:contentStreamLength <= 750"),
                expectedDocCount(11));

        assertQ(qurySolr("SELECT cmis:contentStreamLength FROM cmis:document WHERE cmis:contentStreamLength >  750"),
                expectedDocCount(0));

        assertQ(qurySolr("SELECT cmis:contentStreamLength FROM cmis:document WHERE cmis:contentStreamLength >= 750"),
                expectedDocCount(0));

        assertQ(qurySolr("SELECT cmis:contentStreamLength FROM cmis:document WHERE cmis:contentStreamLength IN     (750)"),
                expectedDocCount(0));

        assertQ(qurySolr("SELECT cmis:contentStreamLength FROM cmis:document WHERE cmis:contentStreamLength NOT IN (750)"),
                expectedDocCount(11));

        assertQ(qurySolr("SELECT cmis:contentStreamLength FROM cmis:document WHERE cmis:contentStreamLength     LIKE '750'"),
                expectedDocCount(0));

        assertQ(qurySolr("SELECT cmis:contentStreamLength FROM cmis:document WHERE cmis:contentStreamLength NOT LIKE '750'"),
                expectedDocCount(11));

        assertQ(qurySolr("SELECT cmis:contentStreamLength FROM cmis:document WHERE cmis:contentStreamLength IS NOT NULL"),
                expectedDocCount(11));

        assertQ(qurySolr("SELECT cmis:contentStreamLength FROM cmis:document WHERE cmis:contentStreamLength IS NULL"),
                expectedDocCount(0));
    }
    public void checkCmisName()
    {
        assertQ(qurySolr("SELECT cmis:name FROM cmis:folder WHERE cmis:name =  'Folder 1'"),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT cmis:name FROM cmis:folder WHERE cmis:name <> 'Folder 1'"),
                expectedDocCount(10));

        assertQ(qurySolr("SELECT cmis:name FROM cmis:folder WHERE cmis:name <  'Folder 1'"),
                expectedDocCount(2));

        assertQ(qurySolr("SELECT cmis:name FROM cmis:folder WHERE cmis:name <= 'Folder 1'"),
                expectedDocCount(3));

        assertQ(qurySolr("SELECT cmis:name FROM cmis:folder WHERE cmis:name >  'Folder 1'"),
                expectedDocCount(8));

        assertQ(qurySolr("SELECT cmis:name FROM cmis:folder WHERE cmis:name >= 'Folder 1'"),
                expectedDocCount(9));

        assertQ(qurySolr("SELECT cmis:name FROM cmis:folder WHERE cmis:name IN     ('Folder 1')"),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT cmis:name FROM cmis:folder WHERE cmis:name NOT IN ('Folder 1')"),
                expectedDocCount(10));

        assertQ(qurySolr("SELECT cmis:name FROM cmis:folder WHERE cmis:name     LIKE 'Folder 1'"),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT cmis:name FROM cmis:folder WHERE cmis:name NOT LIKE 'Folder 1'"),
                expectedDocCount(10));

        assertQ(qurySolr("SELECT cmis:name FROM cmis:folder WHERE cmis:name IS NOT NULL"),
                expectedDocCount(11));

        assertQ(qurySolr("SELECT cmis:name FROM cmis:folder WHERE cmis:name IS     NULL"),
                expectedDocCount(0));

        assertQ(qurySolr("SELECT cmis:name FROM cmis:document WHERE cmis:name =  'Alfresco Tutorial'"),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT cmis:name FROM cmis:document WHERE cmis:name <> 'Alfresco Tutorial'"),
                expectedDocCount(10));

        assertQ(qurySolr("SELECT cmis:name FROM cmis:document WHERE cmis:name <  'Alfresco Tutorial'"),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT cmis:name FROM cmis:document WHERE cmis:name <= 'Alfresco Tutorial'"),
                expectedDocCount(2));

        assertQ(qurySolr("SELECT cmis:name FROM cmis:document WHERE cmis:name >  'Alfresco Tutorial'"),
                expectedDocCount(9));

        assertQ(qurySolr("SELECT cmis:name FROM cmis:document WHERE cmis:name >= 'Alfresco Tutorial'"),
                expectedDocCount(10));

        assertQ(qurySolr("SELECT cmis:name FROM cmis:document WHERE cmis:name IN     ('Alfresco Tutorial')"),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT cmis:name FROM cmis:document WHERE cmis:name NOT IN ('Alfresco Tutorial')"),
                expectedDocCount(10));

        assertQ(qurySolr("SELECT cmis:name FROM cmis:document WHERE cmis:name     LIKE 'Alfresco Tutorial'"),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT cmis:name FROM cmis:document WHERE cmis:name NOT LIKE 'Alfresco Tutorial'"),
                expectedDocCount(10));

        assertQ(qurySolr("SELECT cmis:name FROM cmis:document WHERE cmis:name IS NOT NULL"),
                expectedDocCount(11));

        assertQ(qurySolr("SELECT cmis:name FROM cmis:document WHERE cmis:name IS     NULL"),
                expectedDocCount(0));
    }
    @Test
    public void checkCmisCreatedBy()
    {
        assertQ(qurySolr("SELECT cmis:createdBy FROM cmis:document WHERE cmis:createdBy =  'System'"),
                expectedDocCount(11));

        assertQ(qurySolr("SELECT cmis:createdBy FROM cmis:document WHERE cmis:createdBy <> 'System'"),
                expectedDocCount(0));

        assertQ(qurySolr("SELECT cmis:createdBy FROM cmis:document WHERE cmis:createdBy <  'System'"),
                expectedDocCount(0));

        assertQ(qurySolr("SELECT cmis:createdBy FROM cmis:document WHERE cmis:createdBy <= 'System'"),
                expectedDocCount(11));

        assertQ(qurySolr("SELECT cmis:createdBy FROM cmis:document WHERE cmis:createdBy >  'System'"),
                expectedDocCount(0));

        assertQ(qurySolr("SELECT cmis:createdBy FROM cmis:document WHERE cmis:createdBy >= 'System'"),
                expectedDocCount(11));

        assertQ(qurySolr("SELECT cmis:createdBy FROM cmis:document WHERE cmis:createdBy IN ('System')"),
                expectedDocCount(11));

        assertQ(qurySolr("SELECT cmis:createdBy FROM cmis:document WHERE cmis:createdBy NOT IN ('System')"),
                expectedDocCount(0));

        assertQ(qurySolr("SELECT cmis:createdBy FROM cmis:document WHERE cmis:createdBy LIKE 'System'"),
                expectedDocCount(11));

        assertQ(qurySolr("SELECT cmis:createdBy FROM cmis:document WHERE cmis:createdBy NOT LIKE 'System'"),
                expectedDocCount(0));

        assertQ(qurySolr("SELECT cmis:createdBy FROM cmis:document WHERE cmis:createdBy IS NOT NULL"),
                expectedDocCount(11));

        assertQ(qurySolr("SELECT cmis:createdBy FROM cmis:document WHERE cmis:createdBy IS     NULL"),
                expectedDocCount(0));
    }
    @Test
    public void checkCmisObjectTypeId()
    {
        assertQ(qurySolr("SELECT cmis:objectTypeId FROM cmis:document WHERE cmis:objectTypeId =  'cmis:document'"),
                expectedDocCount(10));

        assertQ(qurySolr("SELECT cmis:objectTypeId FROM cmis:document WHERE cmis:objectTypeId <> 'cmis:document'"),
                expectedDocCount(1));


        assertQ(qurySolr("SELECT cmis:objectTypeId FROM cmis:document WHERE cmis:objectTypeId IN     ('cmis:document')"),
                expectedDocCount(10));
        assertQ(qurySolr("SELECT cmis:objectTypeId FROM cmis:document WHERE cmis:objectTypeId NOT IN ('cmis:document')"),
                expectedDocCount(1));
        assertQ(qurySolr("SELECT cmis:objectTypeId FROM cmis:document WHERE cmis:objectTypeId IS NOT NULL"),
                expectedDocCount(11));
        assertQ(qurySolr("SELECT cmis:objectTypeId FROM cmis:document WHERE cmis:objectTypeId IS  NULL"),
                expectedDocCount(0));

        assertQ(qurySolr("SELECT cmis:objectTypeId FROM cmis:folder WHERE cmis:objectTypeId =  'cmis:folder'"),
                expectedDocCount(11));

        assertQ(qurySolr("SELECT cmis:objectTypeId FROM cmis:folder WHERE cmis:objectTypeId <> 'cmis:folder'"),
                expectedDocCount(0));

        assertQ(qurySolr("SELECT cmis:objectTypeId FROM cmis:folder WHERE cmis:objectTypeId IN     ('cmis:folder')"),
                expectedDocCount(11));

        assertQ(qurySolr("SELECT cmis:objectTypeId FROM cmis:folder WHERE cmis:objectTypeId NOT IN ('cmis:folder')"),
                expectedDocCount(0));

        assertQ(qurySolr("SELECT cmis:objectTypeId FROM cmis:folder WHERE cmis:objectTypeId IS NOT NULL"),
                expectedDocCount(11));

        assertQ(qurySolr("SELECT cmis:objectTypeId FROM cmis:folder WHERE cmis:objectTypeId IS     NULL"),
                expectedDocCount(0));
    }
    @Test
    public void checkCmisObjecId()
    {
        assertQ(qurySolr("SELECT cmis:objectId FROM cmis:folder WHERE cmis:objectId =  '"
                                + testCMISFolder00NodeRef + "'"),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT cmis:objectId FROM cmis:folder WHERE cmis:objectId <> '"
                                + testCMISFolder00NodeRef + "'"),
                expectedDocCount(10));

        assertQ(qurySolr("SELECT cmis:objectId FROM cmis:folder WHERE cmis:objectId IN     ('"
                                + testCMISFolder00NodeRef + "')"),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT cmis:objectId FROM cmis:folder WHERE cmis:objectId  NOT IN('"
                                + testCMISFolder00NodeRef + "')"),
                expectedDocCount(10));

        assertQ(qurySolr("SELECT cmis:objectId FROM cmis:folder WHERE IN_FOLDER('" + testCMISFolder00NodeRef + "')"),
                expectedDocCount(2));

        assertQ(qurySolr("SELECT cmis:objectId FROM cmis:folder WHERE IN_TREE  ('" + testCMISFolder00NodeRef+ "')"),
                expectedDocCount(6));

        assertQ(qurySolr("SELECT cmis:objectId FROM cmis:folder WHERE cmis:objectId IS NOT NULL"),
                expectedDocCount(11));

        assertQ(qurySolr("SELECT cmis:objectId FROM cmis:folder WHERE cmis:objectId IS     NULL"),
                expectedDocCount(0));

        assertQ(qurySolr("SELECT cmis:objectId FROM cmis:folder WHERE cmis:objectId =  '"
                                + testCMISFolder00NodeRef + ";1.0'"),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT cmis:objectId FROM cmis:folder WHERE cmis:objectId <> '"
                                + testCMISFolder00NodeRef + ";1.0'"),
                expectedDocCount(10));

        assertQ(qurySolr("SELECT cmis:objectId FROM cmis:folder WHERE cmis:objectId IN     ('"
                                + testCMISFolder00NodeRef + ";1.0')"),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT cmis:objectId FROM cmis:folder WHERE cmis:objectId  NOT IN('"
                                + testCMISFolder00NodeRef + ";1.0')"),
                expectedDocCount(10));

        String id = testCMISContent00NodeRef.toString();

        assertQ(qurySolr("SELECT cmis:objectId FROM cmis:document WHERE cmis:objectId =  '"
                                + id + "'"),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT cmis:objectId FROM cmis:document WHERE cmis:objectId <> '"
                                + id + "'"),
                expectedDocCount(10));

        assertQ(qurySolr("SELECT cmis:objectId FROM cmis:document WHERE cmis:objectId IN     ('" + id + "')"),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT cmis:objectId FROM cmis:document WHERE cmis:objectId  NOT IN('" + id + "')"),
                expectedDocCount(10));

        assertQ(qurySolr("SELECT cmis:objectId FROM cmis:document WHERE cmis:objectId IS NOT NULL"),
                expectedDocCount(11));

        assertQ(qurySolr("SELECT cmis:objectId FROM cmis:document WHERE cmis:objectId IS     NULL"),
                expectedDocCount(0));

        id = testCMISContent00NodeRef + ";1.0";

        assertQ(qurySolr("SELECT cmis:objectId FROM cmis:document WHERE cmis:objectId =  '"
                                + id + "'"),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT cmis:objectId FROM cmis:document WHERE cmis:objectId <> '"
                                + id + "'"),
                expectedDocCount(10));

        assertQ(qurySolr("SELECT cmis:objectId FROM cmis:document WHERE cmis:objectId IN     ('" + id + "')"),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT cmis:objectId FROM cmis:document WHERE cmis:objectId  NOT IN('" + id + "')"),
                expectedDocCount(10));

        assertQ(qurySolr("SELECT cmis:objectId FROM cmis:document WHERE cmis:objectId IS NOT NULL"),
                expectedDocCount(11));

        assertQ(qurySolr("SELECT cmis:objectId FROM cmis:document WHERE cmis:objectId IS     NULL"),
                expectedDocCount(0));
    }
    @Test
    public void checkCmisTextPredicates()
    {
        assertQ(qurySolr("SELECT * FROM cmis:folder WHERE cmis:name IS NOT NULL AND cmis:name = 'Folder 1'"),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT * FROM cmis:folder WHERE cmis:name IS NOT NULL AND cmis:name = 'Folder 9'"),
                expectedDocCount(0));

        assertQ(qurySolr("SELECT * FROM cmis:folder WHERE cmis:name IS NOT NULL AND cmis:name = 'Folder 9\\''"),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT * FROM cmis:folder WHERE cmis:name IS NOT NULL AND NOT cmis:name = 'Folder 1'"),
                expectedDocCount(10));

        assertQ(qurySolr("SELECT * FROM cmis:folder WHERE cmis:name IS NOT NULL AND 'Folder 1' = ANY cmis:name"),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT * FROM cmis:folder WHERE cmis:name IS NOT NULL AND NOT cmis:name <> 'Folder 1'"),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT * FROM cmis:folder WHERE cmis:name IS NOT NULL AND cmis:name <> 'Folder 1'"),
                expectedDocCount(10));

        assertQ(qurySolr("SELECT * FROM cmis:folder WHERE cmis:name IS NOT NULL AND cmis:name <  'Folder 1'"),
                expectedDocCount(2));

        assertQ(qurySolr("SELECT * FROM cmis:folder WHERE cmis:name IS NOT NULL AND cmis:name <= 'Folder 1'"),
                expectedDocCount(3));

        assertQ(qurySolr("SELECT * FROM cmis:folder WHERE cmis:name IS NOT NULL AND cmis:name >  'Folder 1'"),
                expectedDocCount(8));

        assertQ(qurySolr("SELECT * FROM cmis:folder WHERE cmis:name IS NOT NULL AND cmis:name >= 'Folder 1'"),
                expectedDocCount(9));

        assertQ(qurySolr("SELECT * FROM cmis:folder WHERE cmis:name IS NOT NULL AND cmis:name IN ('Folder 1', '1')"),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT * FROM cmis:folder WHERE cmis:name IS NOT NULL AND cmis:name NOT IN ('Folder 1', 'Folder 9\\'')"),
                expectedDocCount(9));

        assertQ(qurySolr("SELECT * FROM cmis:folder WHERE cmis:name IS NOT NULL AND ANY cmis:name IN ('Folder 1', 'Folder 9\\'')"),
                expectedDocCount(2));

        assertQ(qurySolr("SELECT * FROM cmis:folder WHERE cmis:name IS NOT NULL AND ANY cmis:name NOT IN ('2', '3')"),
                expectedDocCount(11));

        assertQ(qurySolr("SELECT * FROM cmis:folder WHERE cmis:name IS NOT NULL AND cmis:name LIKE 'Folder 1'"),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT * FROM cmis:folder WHERE cmis:name IS NOT NULL AND cmis:name LIKE 'Fol%'"),
                expectedDocCount(10));

        assertQ(qurySolr("SELECT * FROM cmis:folder WHERE cmis:name IS NOT NULL AND cmis:name LIKE 'F_l_e_ 1'"),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT * FROM cmis:folder WHERE cmis:name IS NOT NULL AND cmis:name NOT LIKE 'F_l_e_ 1'"),
                expectedDocCount(10));

        assertQ(qurySolr("SELECT * FROM cmis:folder WHERE cmis:name IS NOT NULL AND cmis:name LIKE 'F_l_e_ %'"),
                expectedDocCount(10));

        assertQ(qurySolr("SELECT * FROM cmis:folder WHERE cmis:name IS NOT NULL AND cmis:name NOT LIKE 'F_l_e_ %'"),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT * FROM cmis:folder WHERE cmis:name IS NOT NULL AND cmis:name LIKE 'F_l_e_ _'"),
                expectedDocCount(9));

        assertQ(qurySolr("SELECT * FROM cmis:folder WHERE cmis:name IS NOT NULL AND cmis:name NOT LIKE 'F_l_e_ _'"),
                expectedDocCount(2));
    }
    @Test
    public void checkCmisSimpleConjunction()
    {
        assertQ(qurySolr("SELECT * FROM cmis:folder WHERE cmis:name IS NOT NULL AND cmis:name = 'Folder 1'"),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT * FROM cmis:folder WHERE cmis:name IS NOT NULL AND cmis:name = 'Folder'"),
                expectedDocCount(0));
    }
    @Test
    public void checkCmisSimpleDisjunction()
    {
        assertQ(qurySolr("SELECT * FROM cmis:folder WHERE cmis:name = 'Folder 1'"),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT * FROM cmis:folder WHERE cmis:name = 'Folder 2'"),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT * FROM cmis:folder WHERE cmis:name = 'Folder 1' OR cmis:name = 'Folder 2'"),
                expectedDocCount(2));
    }
    @Test
    public void checkCmisExists()
    {
        assertQ(qurySolr("SELECT * FROM cmis:folder WHERE cmis:name IS NOT NULL"),
                expectedDocCount(11));

        assertQ(qurySolr("SELECT * FROM cmis:folder WHERE cmis:name IS NULL"),
                expectedDocCount(0));

        assertQ(qurySolr("SELECT * FROM cmis:document WHERE cmis:name IS NOT NULL"),
                expectedDocCount(11));

        assertQ(qurySolr("SELECT * FROM cmis:document WHERE cmis:name IS NULL"),
                expectedDocCount(0));
    }
    @Test
    public void checkInTree()
    {
        assertQ(qurySolr("SELECT * FROM cmis:folder WHERE IN_TREE('" + testCMISFolder00NodeRef + "')"),
                expectedDocCount(6));

        assertQ(qurySolr("SELECT * FROM cmis:folder F WHERE IN_TREE(F, '" + testCMISFolder00NodeRef + "')"),
                expectedDocCount(6));

        assertQ(qurySolr("SELECT D.*, O.* FROM cmis:document AS D JOIN cm:ownable AS O ON D.cmis:objectId = O.cmis:objectId WHERE IN_TREE(D, '"
                                + testCMISBaseFolderNodeRef + "')"),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT * FROM cmis:folder WHERE IN_TREE('woof://woof/woof')"),
                expectedDocCount(0));


        assertQ(qurySolr("SELECT * FROM cmis:folder WHERE IN_TREE('woof://woof/woof;woof')"),
                expectedDocCount(0));
    }
    @Test
    public void checkLikeEscaping()
    {
        assertQ(qurySolr("SELECT cmis:name FROM cmis:document WHERE cmis:name     LIKE 'Alfresco Tutorial'"),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT cmis:name FROM cmis:document WHERE cmis:name     LIKE 'Alfresco Tutoria_'"),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT cmis:name FROM cmis:document WHERE cmis:name     LIKE 'Alfresco T_______'"),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT cmis:name FROM cmis:document WHERE cmis:name     LIKE 'Alfresco T______\\_'"),
                expectedDocCount(0));

        assertQ(qurySolr("SELECT cmis:name FROM cmis:document WHERE cmis:name     LIKE 'Alfresco T%'"),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT cmis:name FROM cmis:document WHERE cmis:name     LIKE 'Alfresco'"),
                expectedDocCount(0));

        assertQ(qurySolr("SELECT cmis:name FROM cmis:document WHERE cmis:name     LIKE 'Alfresco%'"),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT cmis:name FROM cmis:document WHERE cmis:name     LIKE 'Alfresco T\\%'"),
                expectedDocCount(0));

        assertQ(qurySolr("SELECT cmis:name FROM cmis:document WHERE cmis:name     LIKE 'GG*GG'"),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT cmis:name FROM cmis:document WHERE cmis:name     LIKE '__*__'"),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT cmis:name FROM cmis:document WHERE cmis:name     LIKE '%*%'"),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT cmis:name FROM cmis:document WHERE cmis:name     LIKE 'HH?HH'"),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT cmis:name FROM cmis:document WHERE cmis:name     LIKE '__?__'"),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT cmis:name FROM cmis:document WHERE cmis:name     LIKE '%?%'"),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT cmis:name FROM cmis:document WHERE cmis:name     LIKE 'AA%'"),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT cmis:name FROM cmis:document WHERE cmis:name     LIKE 'AA\\%'"),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT cmis:name FROM cmis:document WHERE cmis:name     LIKE 'A%'"),
                expectedDocCount(2));

        assertQ(qurySolr("SELECT cmis:name FROM cmis:document WHERE cmis:name     LIKE 'a%'"),
                expectedDocCount(2));

        assertQ(qurySolr("SELECT cmis:name FROM cmis:document WHERE cmis:name     LIKE 'A\\%'"),
                expectedDocCount(0));

        assertQ(qurySolr("SELECT cmis:name FROM cmis:document WHERE cmis:name     LIKE 'BB_'"),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT cmis:name FROM cmis:document WHERE cmis:name     LIKE 'BB\\_'"),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT cmis:name FROM cmis:document WHERE cmis:name     LIKE 'B__'"),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT cmis:name FROM cmis:document WHERE cmis:name     LIKE 'B_\\_'"),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT cmis:name FROM cmis:document WHERE cmis:name     LIKE 'B\\_\\_'"),
                expectedDocCount(0));

        assertQ(qurySolr("SELECT cmis:name FROM cmis:document WHERE cmis:name     LIKE 'CC\\\\'"),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT cmis:name FROM cmis:document WHERE cmis:name     LIKE 'DD\\''"),
                expectedDocCount(1));
    }

    @Test
    public void checkDateFormatting()
    {
        assertQ(qurySolr("SELECT * FROM cm:lockable L WHERE L.cm:expiryDate =  TIMESTAMP '2012-12-12T12:12:12.012Z'"),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT * FROM cm:lockable L WHERE L.cm:expiryDate =  TIMESTAMP '2012-012-12T12:12:12.012Z'"),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT * FROM cm:lockable L WHERE L.cm:expiryDate =  TIMESTAMP '2012-2-12T12:12:12.012Z'"),
                expectedDocCount(0));
    }
    @Test
    public void checkAspectJoin()
    {
        assertQ(qurySolr("select o.*, t.* from ( cm:ownable o join cm:titled t on o.cmis:objectId = t.cmis:objectId JOIN cmis:document AS D ON D.cmis:objectId = o.cmis:objectId  ) where o.cm:owner = 'andy' and t.cm:title = 'Alfresco tutorial' and CONTAINS(D, '\\'jumped\\'') and D.cmis:contentStreamLength <> 2"),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT * FROM cm:ownable"),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT * FROM cm:ownable where cm:owner = 'andy'"),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT * FROM cm:ownable where cm:owner = 'bob'"),
                expectedDocCount(0));

        assertQ(qurySolr("SELECT D.*, O.* FROM cmis:document AS D JOIN cm:ownable AS O ON D.cmis:objectId = O.cmis:objectId"),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT D.*, O.*, T.* FROM cmis:document AS D JOIN cm:ownable AS O ON D.cmis:objectId = O.cmis:objectId JOIN cm:titled AS T ON T.cmis:objectId = D.cmis:objectId"),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT D.*, O.* FROM cm:ownable O JOIN cmis:document D ON D.cmis:objectId = O.cmis:objectId"),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT D.*, F.* FROM cmis:folder F JOIN cmis:document D ON D.cmis:objectId = F.cmis:objectId"),
                expectedDocCount(0));

        assertQ(qurySolr("SELECT O.*, T.* FROM cm:ownable O JOIN cm:titled T ON O.cmis:objectId = T.cmis:objectId"),
                expectedDocCount(1));

        assertQ(qurySolr("select o.*, t.* from cm:ownable o join cm:titled t on o.cmis:objectId = t.cmis:objectId"),
                expectedDocCount(1));

        assertQ(qurySolr("sElEcT o.*, T.* fRoM cm:ownable o JoIn cm:titled T oN o.cmis:objectId = T.cmis:objectId"),
                expectedDocCount(1));

        assertQ(qurySolr("select o.*, t.* from ( cm:ownable o join cm:titled t on o.cmis:objectId = t.cmis:objectId )"),
                expectedDocCount(1));

        assertQ(qurySolr("select o.*, t.* from ( cm:ownable o join cm:titled t on o.cmis:objectId = t.cmis:objectId  JOIN cmis:document AS D ON D.cmis:objectId = o.cmis:objectId  )"),
                expectedDocCount(1));

        assertQ(qurySolr("select o.*, t.* from ( cm:ownable o join cm:titled t on o.cmis:objectId = t.cmis:objectId JOIN cmis:document AS D ON D.cmis:objectId = o.cmis:objectId ) where o.cm:owner = 'andy' and t.cm:title = 'Alfresco tutorial' and CONTAINS(D, '\\'jumped\\'') and D.cmis:contentStreamLength <> 2"),
                expectedDocCount(1));

        assertQ(qurySolr("select o.*, t.* from ( cm:ownable o join cm:titled t on o.cmis:objectId = t.cmis:objectId JOIN cmis:document AS D ON D.cmis:objectId = o.cmis:objectId ) where o.cm:owner = 'andy' and t.cm:title = 'Alfresco tutorial' and CONTAINS(D, 'jumped') and D.cmis:contentStreamLength <> 2"),
                expectedDocCount(1));
    }
    @Test
    public void checkFTS()
    {

        assertQ(qurySolr("SELECT SCORE()as ONE, SCORE()as TWO, D.* FROM cmis:document D WHERE CONTAINS('\\'zebra\\'')"),
                expectedDocCount(10));

        assertQ(qurySolr("SELECT * FROM cmis:document WHERE CONTAINS('\\'zebra\\'')"),
                expectedDocCount(10));

        assertQ(qurySolr("SELECT * FROM cmis:document WHERE CONTAINS('\\'quick\\'')"),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT * FROM cmis:document WHERE CONTAINS('\\'quick\\'')"),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT * FROM cmis:document D WHERE CONTAINS(D, 'cmis:name:\\'Tutorial\\'')"),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT cmis:name as BOO FROM cmis:document D WHERE CONTAINS('BOO:\\'Tutorial\\'')"),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT * FROM cmis:document D WHERE CONTAINS('TEXT:\\'zebra\\'')"),
                expectedDocCount(10));

        assertQ(qurySolr("SELECT * FROM cmis:document D WHERE CONTAINS('d:content:\\'zebra\\'')"),
                expectedDocCount(10));
    }
    @Test
    public void checkFTSConnectives()
    {
        assertQ(qurySolr("SELECT * FROM cmis:document where contains('\\'two\\' OR \\'zebra\\'')"),
                expectedDocCount(10));
        assertQ(qurySolr("SELECT * FROM cmis:document where contains('\\'two\\' or \\'zebra\\'')"),
                expectedDocCount(10));

        assertQ(qurySolr("SELECT * FROM cmis:document where contains('\\'two\\' \\'zebra\\'')"),
                expectedDocCount(1));
        assertQ(qurySolr("SELECT * FROM cmis:document where contains('\\'two\\' and \\'zebra\\'')"),
                expectedDocCount(1));
        assertQ(qurySolr("SELECT * FROM cmis:document where contains('\\'two\\' or \\'zebra\\'')"),
                expectedDocCount(10));
        assertQ(qurySolr("SELECT * FROM cmis:document where contains('\\'two\\'  \\'zebra\\'')"),
                expectedDocCount(1));
    }
}