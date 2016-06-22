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

package org.alfresco.solr.query;

import org.alfresco.repo.search.adaptor.lucene.QueryConstants;
import org.apache.lucene.util.LuceneTestCase;
import org.apache.solr.SolrTestCaseJ4;
import org.junit.Test;


@LuceneTestCase.SuppressCodecs({"Appending","Lucene3x","Lucene40","Lucene41","Lucene42","Lucene43", "Lucene44", "Lucene45","Lucene46","Lucene47","Lucene48","Lucene49"})
@SolrTestCaseJ4.SuppressSSL
public class AlfrescoCMISQParserPluginTest extends LoadCMISData implements QueryConstants 
{
    @Test
    public void dataChecks() throws Exception {

        /********************TEST CMIS BASIC **************************/

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q", "SELECT * from cmis:folder"), null),
                "*[count(//doc)=11]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",  "SELECT * from cmis:document"), null),
                "*[count(//doc)=11]");


        /************** Test CMIS Parent ID ***********************/

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q", "SELECT cmis:parentId FROM cmis:folder WHERE cmis:parentId =  '"
                        + testCMISBaseFolderNodeRef + "'"), null),
                "*[count(//doc)=4]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q", "SELECT cmis:parentId FROM cmis:folder WHERE cmis:parentId <> '"
                        + testCMISBaseFolderNodeRef + "'"), null),
                "*[count(//doc)=7]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q", "SELECT cmis:parentId FROM cmis:folder WHERE cmis:parentId IN     ('"
                        + testCMISBaseFolderNodeRef + "')"), null),
                "*[count(//doc)=4]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q", "SELECT cmis:parentId FROM cmis:folder WHERE cmis:parentId NOT IN ('"
                        + testCMISBaseFolderNodeRef + "')"), null),
                "*[count(//doc)=7]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q", "SELECT cmis:parentId FROM cmis:folder WHERE cmis:parentId IS NOT NULL"), null),
                "*[count(//doc)=11]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",  "SELECT cmis:parentId FROM cmis:folder WHERE cmis:parentId IS NULL"), null),
                "*[count(//doc)=0]");

        /************** checkCmisContentStreamFileName ***********************/

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",  "SELECT cmis:contentStreamFileName FROM cmis:document WHERE cmis:contentStreamFileName =  'Alfresco Tutorial'"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",   "SELECT cmis:contentStreamFileName FROM cmis:document WHERE cmis:contentStreamFileName =  'AA%'"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmis:contentStreamFileName FROM cmis:document WHERE cmis:contentStreamFileName =  'BB_'"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmis:contentStreamFileName FROM cmis:document WHERE cmis:contentStreamFileName =  'CC\\\\'"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmis:contentStreamFileName FROM cmis:document WHERE cmis:contentStreamFileName =  'DD\\''"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmis:contentStreamFileName FROM cmis:document WHERE cmis:contentStreamFileName =  'EE.aa'"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmis:contentStreamFileName FROM cmis:document WHERE cmis:contentStreamFileName =  'FF.EE'"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmis:contentStreamFileName FROM cmis:document WHERE cmis:contentStreamFileName =  'GG*GG'"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmis:contentStreamFileName FROM cmis:document WHERE cmis:contentStreamFileName =  'HH?HH'"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmis:contentStreamFileName FROM cmis:document WHERE cmis:contentStreamFileName =  'aa'"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                "SELECT cmis:contentStreamFileName FROM cmis:document WHERE cmis:contentStreamFileName =  'Alfresco Tutorial'"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmis:contentStreamFileName FROM cmis:document WHERE cmis:contentStreamFileName <> 'Alfresco Tutorial'"), null),
                "*[count(//doc)=10]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmis:contentStreamFileName FROM cmis:document WHERE cmis:contentStreamFileName <  'Alfresco Tutorial'"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmis:contentStreamFileName FROM cmis:document WHERE cmis:contentStreamFileName <= 'Alfresco Tutorial'"), null),
                "*[count(//doc)=2]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmis:contentStreamFileName FROM cmis:document WHERE cmis:contentStreamFileName >  'Alfresco Tutorial'"), null),
                "*[count(//doc)=9]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmis:contentStreamFileName FROM cmis:document WHERE cmis:contentStreamFileName >= 'Alfresco Tutorial'"), null),
                "*[count(//doc)=10]");


        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmis:contentStreamFileName FROM cmis:document WHERE cmis:contentStreamFileName IN     ('Alfresco Tutorial')"), null),
                "*[count(//doc)=1]");


        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmis:contentStreamFileName FROM cmis:document WHERE cmis:contentStreamFileName NOT IN ('Alfresco Tutorial')"), null),
                "*[count(//doc)=10]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                "SELECT cmis:contentStreamFileName FROM cmis:document WHERE cmis:contentStreamFileName     LIKE 'Alfresco Tutorial'"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmis:contentStreamFileName FROM cmis:document WHERE cmis:contentStreamFileName NOT LIKE 'Alfresco Tutorial'"), null),
                "*[count(//doc)=10]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmis:contentStreamFileName FROM cmis:document WHERE cmis:contentStreamFileName IS NOT NULL"), null),
                "*[count(//doc)=11]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmis:contentStreamFileName FROM cmis:document WHERE cmis:contentStreamFileName IS     NULL"), null),
                "*[count(//doc)=0]");

        /************** checkCmisContentStreamMimeType ***********************/

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmis:contentStreamMimeType FROM cmis:document WHERE cmis:contentStreamMimeType =  'text/plain'"), null),
                "*[count(//doc)=11]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmis:contentStreamMimeType FROM cmis:document WHERE cmis:contentStreamMimeType <> 'text/plain'"), null),
                "*[count(//doc)=0]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmis:contentStreamMimeType FROM cmis:document WHERE cmis:contentStreamMimeType <  'text/plain'"), null),
                "*[count(//doc)=0]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmis:contentStreamMimeType FROM cmis:document WHERE cmis:contentStreamMimeType <= 'text/plain'"), null),
                "*[count(//doc)=11]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmis:contentStreamMimeType FROM cmis:document WHERE cmis:contentStreamMimeType >  'text/plain'"), null),
                "*[count(//doc)=0]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmis:contentStreamMimeType FROM cmis:document WHERE cmis:contentStreamMimeType >= 'text/plain'"), null),
                "*[count(//doc)=11]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmis:contentStreamMimeType FROM cmis:document WHERE cmis:contentStreamMimeType IN     ('text/plain')"), null),
                "*[count(//doc)=11]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmis:contentStreamMimeType FROM cmis:document WHERE cmis:contentStreamMimeType NOT IN ('text/plain')"), null),
                "*[count(//doc)=0]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmis:contentStreamMimeType FROM cmis:document WHERE cmis:contentStreamMimeType     LIKE 'text/plain'"), null),
                "*[count(//doc)=11]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmis:contentStreamMimeType FROM cmis:document WHERE cmis:contentStreamMimeType NOT LIKE 'text/plain'"), null),
                "*[count(//doc)=0]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmis:contentStreamMimeType FROM cmis:document WHERE cmis:contentStreamMimeType IS NOT NULL"), null),
                "*[count(//doc)=11]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmis:contentStreamMimeType FROM cmis:document WHERE cmis:contentStreamMimeType IS     NULL"), null),
                "*[count(//doc)=0]");

        /************** checkCmisContentStreamLength ***********************/

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmis:contentStreamLength FROM cmis:document WHERE cmis:contentStreamLength =  750"), null),
                "*[count(//doc)=0]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmis:contentStreamLength FROM cmis:document WHERE cmis:contentStreamLength <> 750"), null),
                "*[count(//doc)=11]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmis:contentStreamLength FROM cmis:document WHERE cmis:contentStreamLength <  750"), null),
                "*[count(//doc)=11]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmis:contentStreamLength FROM cmis:document WHERE cmis:contentStreamLength <= 750"), null),
                "*[count(//doc)=11]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmis:contentStreamLength FROM cmis:document WHERE cmis:contentStreamLength >  750"), null),
                "*[count(//doc)=0]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmis:contentStreamLength FROM cmis:document WHERE cmis:contentStreamLength >= 750"), null),
                "*[count(//doc)=0]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmis:contentStreamLength FROM cmis:document WHERE cmis:contentStreamLength IN     (750)"), null),
                "*[count(//doc)=0]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmis:contentStreamLength FROM cmis:document WHERE cmis:contentStreamLength NOT IN (750)"), null),
                "*[count(//doc)=11]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmis:contentStreamLength FROM cmis:document WHERE cmis:contentStreamLength     LIKE '750'"), null),
                "*[count(//doc)=0]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmis:contentStreamLength FROM cmis:document WHERE cmis:contentStreamLength NOT LIKE '750'"), null),
                "*[count(//doc)=11]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmis:contentStreamLength FROM cmis:document WHERE cmis:contentStreamLength IS NOT NULL"), null),
                "*[count(//doc)=11]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmis:contentStreamLength FROM cmis:document WHERE cmis:contentStreamLength IS NULL"), null),
                "*[count(//doc)=0]");

        /************** checkCmisName ***********************/

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmis:name FROM cmis:folder WHERE cmis:name =  'Folder 1'"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmis:name FROM cmis:folder WHERE cmis:name <> 'Folder 1'"), null),
                "*[count(//doc)=10]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmis:name FROM cmis:folder WHERE cmis:name <  'Folder 1'"), null),
                "*[count(//doc)=2]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmis:name FROM cmis:folder WHERE cmis:name <= 'Folder 1'"), null),
                "*[count(//doc)=3]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmis:name FROM cmis:folder WHERE cmis:name >  'Folder 1'"), null),
                "*[count(//doc)=8]");


        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmis:name FROM cmis:folder WHERE cmis:name >= 'Folder 1'"), null),
                "*[count(//doc)=9]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmis:name FROM cmis:folder WHERE cmis:name IN     ('Folder 1')"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmis:name FROM cmis:folder WHERE cmis:name NOT IN ('Folder 1')"), null),
                "*[count(//doc)=10]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmis:name FROM cmis:folder WHERE cmis:name     LIKE 'Folder 1'"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmis:name FROM cmis:folder WHERE cmis:name NOT LIKE 'Folder 1'"), null),
                "*[count(//doc)=10]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmis:name FROM cmis:folder WHERE cmis:name IS NOT NULL"), null),
                "*[count(//doc)=11]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmis:name FROM cmis:folder WHERE cmis:name IS     NULL"), null),
                "*[count(//doc)=0]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmis:name FROM cmis:document WHERE cmis:name =  'Alfresco Tutorial'"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmis:name FROM cmis:document WHERE cmis:name <> 'Alfresco Tutorial'"), null),
                "*[count(//doc)=10]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmis:name FROM cmis:document WHERE cmis:name <  'Alfresco Tutorial'"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmis:name FROM cmis:document WHERE cmis:name <= 'Alfresco Tutorial'"), null),
                "*[count(//doc)=2]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmis:name FROM cmis:document WHERE cmis:name >  'Alfresco Tutorial'"), null),
                "*[count(//doc)=9]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmis:name FROM cmis:document WHERE cmis:name >= 'Alfresco Tutorial'"), null),
                "*[count(//doc)=10]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmis:name FROM cmis:document WHERE cmis:name IN     ('Alfresco Tutorial')"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmis:name FROM cmis:document WHERE cmis:name NOT IN ('Alfresco Tutorial')"), null),
                "*[count(//doc)=10]");


        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmis:name FROM cmis:document WHERE cmis:name     LIKE 'Alfresco Tutorial'"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmis:name FROM cmis:document WHERE cmis:name NOT LIKE 'Alfresco Tutorial'"), null),
                "*[count(//doc)=10]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmis:name FROM cmis:document WHERE cmis:name IS NOT NULL"), null),
                "*[count(//doc)=11]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmis:name FROM cmis:document WHERE cmis:name IS     NULL"), null),
                "*[count(//doc)=0]");

        /************** checkCmisCreatedBy ***********************/

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmis:createdBy FROM cmis:document WHERE cmis:createdBy =  'System'"), null),
                "*[count(//doc)=11]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmis:createdBy FROM cmis:document WHERE cmis:createdBy <> 'System'"), null),
                "*[count(//doc)=0]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmis:createdBy FROM cmis:document WHERE cmis:createdBy <  'System'"), null),
                "*[count(//doc)=0]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmis:createdBy FROM cmis:document WHERE cmis:createdBy <= 'System'"), null),
                "*[count(//doc)=11]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmis:createdBy FROM cmis:document WHERE cmis:createdBy >  'System'"), null),
                "*[count(//doc)=0]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmis:createdBy FROM cmis:document WHERE cmis:createdBy >= 'System'"), null),
                "*[count(//doc)=11]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmis:createdBy FROM cmis:document WHERE cmis:createdBy IN ('System')"), null),
                "*[count(//doc)=11]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmis:createdBy FROM cmis:document WHERE cmis:createdBy NOT IN ('System')"), null),
                "*[count(//doc)=0]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmis:createdBy FROM cmis:document WHERE cmis:createdBy LIKE 'System'"), null),
                "*[count(//doc)=11]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmis:createdBy FROM cmis:document WHERE cmis:createdBy NOT LIKE 'System'"), null),
                "*[count(//doc)=0]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmis:createdBy FROM cmis:document WHERE cmis:createdBy IS NOT NULL"), null),
                "*[count(//doc)=11]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmis:createdBy FROM cmis:document WHERE cmis:createdBy IS     NULL"), null),
                "*[count(//doc)=0]");

        /************** checkCmisObjectTypeId ***********************/

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmis:objectTypeId FROM cmis:document WHERE cmis:objectTypeId =  'cmis:document'"), null),
                "*[count(//doc)=10]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmis:objectTypeId FROM cmis:document WHERE cmis:objectTypeId <> 'cmis:document'"), null),
                "*[count(//doc)=1]");


        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmis:objectTypeId FROM cmis:document WHERE cmis:objectTypeId IN     ('cmis:document')"), null),
                "*[count(//doc)=10]");


        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmis:objectTypeId FROM cmis:document WHERE cmis:objectTypeId NOT IN ('cmis:document')"), null),
                "*[count(//doc)=1]");


        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmis:objectTypeId FROM cmis:document WHERE cmis:objectTypeId IS NOT NULL"), null),
                "*[count(//doc)=11]");


        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmis:objectTypeId FROM cmis:document WHERE cmis:objectTypeId IS  NULL"), null),
                "*[count(//doc)=0]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmis:objectTypeId FROM cmis:folder WHERE cmis:objectTypeId =  'cmis:folder'"), null),
                "*[count(//doc)=11]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmis:objectTypeId FROM cmis:folder WHERE cmis:objectTypeId <> 'cmis:folder'"), null),
                "*[count(//doc)=0]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmis:objectTypeId FROM cmis:folder WHERE cmis:objectTypeId IN     ('cmis:folder')"), null),
                "*[count(//doc)=11]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmis:objectTypeId FROM cmis:folder WHERE cmis:objectTypeId NOT IN ('cmis:folder')"), null),
                "*[count(//doc)=0]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmis:objectTypeId FROM cmis:folder WHERE cmis:objectTypeId IS NOT NULL"), null),
                "*[count(//doc)=11]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmis:objectTypeId FROM cmis:folder WHERE cmis:objectTypeId IS     NULL"), null),
                "*[count(//doc)=0]");

        /************** checkCmisObjecId ***********************/

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmis:objectId FROM cmis:folder WHERE cmis:objectId =  '"
                                + testCMISFolder00NodeRef + "'"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmis:objectId FROM cmis:folder WHERE cmis:objectId <> '"
                                + testCMISFolder00NodeRef + "'"), null),
                "*[count(//doc)=10]");


        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmis:objectId FROM cmis:folder WHERE cmis:objectId IN     ('"
                                + testCMISFolder00NodeRef + "')"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmis:objectId FROM cmis:folder WHERE cmis:objectId  NOT IN('"
                                + testCMISFolder00NodeRef + "')"), null),
                "*[count(//doc)=10]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmis:objectId FROM cmis:folder WHERE IN_FOLDER('" + testCMISFolder00NodeRef + "')"), null),
                "*[count(//doc)=2]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                "SELECT cmis:objectId FROM cmis:folder WHERE IN_TREE  ('" + testCMISFolder00NodeRef+ "')"), null),
                "*[count(//doc)=6]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmis:objectId FROM cmis:folder WHERE cmis:objectId IS NOT NULL"), null),
                "*[count(//doc)=11]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmis:objectId FROM cmis:folder WHERE cmis:objectId IS     NULL"), null),
                "*[count(//doc)=0]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmis:objectId FROM cmis:folder WHERE cmis:objectId =  '"
                                + testCMISFolder00NodeRef + ";1.0'"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmis:objectId FROM cmis:folder WHERE cmis:objectId <> '"
                                + testCMISFolder00NodeRef + ";1.0'"), null),
                "*[count(//doc)=10]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmis:objectId FROM cmis:folder WHERE cmis:objectId IN     ('"
                                + testCMISFolder00NodeRef + ";1.0')"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmis:objectId FROM cmis:folder WHERE cmis:objectId  NOT IN('"
                                + testCMISFolder00NodeRef + ";1.0')"), null),
                "*[count(//doc)=10]");

        String id = testCMISContent00NodeRef.toString();

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmis:objectId FROM cmis:document WHERE cmis:objectId =  '"
                                + id + "'"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmis:objectId FROM cmis:document WHERE cmis:objectId <> '"
                                + id + "'"), null),
                "*[count(//doc)=10]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmis:objectId FROM cmis:document WHERE cmis:objectId IN     ('" + id + "')"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmis:objectId FROM cmis:document WHERE cmis:objectId  NOT IN('" + id + "')"), null),
                "*[count(//doc)=10]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmis:objectId FROM cmis:document WHERE cmis:objectId IS NOT NULL"), null),
                "*[count(//doc)=11]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmis:objectId FROM cmis:document WHERE cmis:objectId IS     NULL"), null),
                "*[count(//doc)=0]");

        id = testCMISContent00NodeRef + ";1.0";

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmis:objectId FROM cmis:document WHERE cmis:objectId =  '"
                                + id + "'"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmis:objectId FROM cmis:document WHERE cmis:objectId <> '"
                                + id + "'"), null),
                "*[count(//doc)=10]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmis:objectId FROM cmis:document WHERE cmis:objectId IN     ('" + id + "')"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmis:objectId FROM cmis:document WHERE cmis:objectId  NOT IN('" + id + "')"), null),
                "*[count(//doc)=10]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmis:objectId FROM cmis:document WHERE cmis:objectId IS NOT NULL"), null),
                "*[count(//doc)=11]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmis:objectId FROM cmis:document WHERE cmis:objectId IS     NULL"), null),
                "*[count(//doc)=0]");

        /************** checkCmisTextPredicates ***********************/

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmis:folder WHERE cmis:name IS NOT NULL AND cmis:name = 'Folder 1'"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmis:folder WHERE cmis:name IS NOT NULL AND cmis:name = 'Folder 9'"), null),
                "*[count(//doc)=0]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmis:folder WHERE cmis:name IS NOT NULL AND cmis:name = 'Folder 9\\''"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmis:folder WHERE cmis:name IS NOT NULL AND NOT cmis:name = 'Folder 1'"), null),
                "*[count(//doc)=10]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmis:folder WHERE cmis:name IS NOT NULL AND 'Folder 1' = ANY cmis:name"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmis:folder WHERE cmis:name IS NOT NULL AND NOT cmis:name <> 'Folder 1'"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmis:folder WHERE cmis:name IS NOT NULL AND cmis:name <> 'Folder 1'"), null),
                "*[count(//doc)=10]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmis:folder WHERE cmis:name IS NOT NULL AND cmis:name <  'Folder 1'"), null),
                "*[count(//doc)=2]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmis:folder WHERE cmis:name IS NOT NULL AND cmis:name <= 'Folder 1'"), null),
                "*[count(//doc)=3]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmis:folder WHERE cmis:name IS NOT NULL AND cmis:name >  'Folder 1'"), null),
                "*[count(//doc)=8]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmis:folder WHERE cmis:name IS NOT NULL AND cmis:name >= 'Folder 1'"), null),
                "*[count(//doc)=9]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmis:folder WHERE cmis:name IS NOT NULL AND cmis:name IN ('Folder 1', '1')"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmis:folder WHERE cmis:name IS NOT NULL AND cmis:name NOT IN ('Folder 1', 'Folder 9\\'')"), null),
                "*[count(//doc)=9]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmis:folder WHERE cmis:name IS NOT NULL AND ANY cmis:name IN ('Folder 1', 'Folder 9\\'')"), null),
                "*[count(//doc)=2]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                "SELECT * FROM cmis:folder WHERE cmis:name IS NOT NULL AND ANY cmis:name NOT IN ('2', '3')"), null),
                "*[count(//doc)=11]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmis:folder WHERE cmis:name IS NOT NULL AND cmis:name LIKE 'Folder 1'"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmis:folder WHERE cmis:name IS NOT NULL AND cmis:name LIKE 'Fol%'"), null),
                "*[count(//doc)=10]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmis:folder WHERE cmis:name IS NOT NULL AND cmis:name LIKE 'F_l_e_ 1'"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmis:folder WHERE cmis:name IS NOT NULL AND cmis:name NOT LIKE 'F_l_e_ 1'"), null),
                "*[count(//doc)=10]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmis:folder WHERE cmis:name IS NOT NULL AND cmis:name LIKE 'F_l_e_ %'"), null),
                "*[count(//doc)=10]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmis:folder WHERE cmis:name IS NOT NULL AND cmis:name NOT LIKE 'F_l_e_ %'"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmis:folder WHERE cmis:name IS NOT NULL AND cmis:name LIKE 'F_l_e_ _'"), null),
                "*[count(//doc)=9]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmis:folder WHERE cmis:name IS NOT NULL AND cmis:name NOT LIKE 'F_l_e_ _'"), null),
                "*[count(//doc)=2]");



        /********* checkCmisSimpleConjunction **********************/

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmis:folder WHERE cmis:name IS NOT NULL AND cmis:name = 'Folder 1'"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmis:folder WHERE cmis:name IS NOT NULL AND cmis:name = 'Folder'"), null),
                "*[count(//doc)=0]");



        /*************** checkCmisSimpleDisjunction *****************/

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                "SELECT * FROM cmis:folder WHERE cmis:name = 'Folder 1'"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmis:folder WHERE cmis:name = 'Folder 2'"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmis:folder WHERE cmis:name = 'Folder 1' OR cmis:name = 'Folder 2'"), null),
                "*[count(//doc)=2]");

        /*************** checkCmisExists *********************/

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmis:folder WHERE cmis:name IS NOT NULL"), null),
                "*[count(//doc)=11]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmis:folder WHERE cmis:name IS NULL"), null),
                "*[count(//doc)=0]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmis:document WHERE cmis:name IS NOT NULL"), null),
                "*[count(//doc)=11]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmis:document WHERE cmis:name IS NULL"), null),
                "*[count(//doc)=0]");

        /********** checkInTree ***************************/

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmis:folder WHERE IN_TREE('" + testCMISFolder00NodeRef + "')"), null),
                "*[count(//doc)=6]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmis:folder F WHERE IN_TREE(F, '" + testCMISFolder00NodeRef + "')"), null),
                "*[count(//doc)=6]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT D.*, O.* FROM cmis:document AS D JOIN cm:ownable AS O ON D.cmis:objectId = O.cmis:objectId WHERE IN_TREE(D, '"
                                + testCMISBaseFolderNodeRef + "')"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmis:folder WHERE IN_TREE('woof://woof/woof')"), null),
                "*[count(//doc)=0]");


        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmis:folder WHERE IN_TREE('woof://woof/woof;woof')"), null),
                "*[count(//doc)=0]");

        /************* checkLikeEscaping *******************/


        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmis:name FROM cmis:document WHERE cmis:name     LIKE 'Alfresco Tutorial'"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmis:name FROM cmis:document WHERE cmis:name     LIKE 'Alfresco Tutoria_'"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmis:name FROM cmis:document WHERE cmis:name     LIKE 'Alfresco T_______'"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmis:name FROM cmis:document WHERE cmis:name     LIKE 'Alfresco T______\\_'"), null),
                "*[count(//doc)=0]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmis:name FROM cmis:document WHERE cmis:name     LIKE 'Alfresco T%'"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmis:name FROM cmis:document WHERE cmis:name     LIKE 'Alfresco'"), null),
                "*[count(//doc)=0]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmis:name FROM cmis:document WHERE cmis:name     LIKE 'Alfresco%'"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmis:name FROM cmis:document WHERE cmis:name     LIKE 'Alfresco T\\%'"), null),
                "*[count(//doc)=0]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmis:name FROM cmis:document WHERE cmis:name     LIKE 'GG*GG'"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmis:name FROM cmis:document WHERE cmis:name     LIKE '__*__'"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmis:name FROM cmis:document WHERE cmis:name     LIKE '%*%'"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmis:name FROM cmis:document WHERE cmis:name     LIKE 'HH?HH'"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmis:name FROM cmis:document WHERE cmis:name     LIKE '__?__'"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmis:name FROM cmis:document WHERE cmis:name     LIKE '%?%'"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmis:name FROM cmis:document WHERE cmis:name     LIKE 'AA%'"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmis:name FROM cmis:document WHERE cmis:name     LIKE 'AA\\%'"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmis:name FROM cmis:document WHERE cmis:name     LIKE 'A%'"), null),
                "*[count(//doc)=2]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmis:name FROM cmis:document WHERE cmis:name     LIKE 'a%'"), null),
                "*[count(//doc)=2]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmis:name FROM cmis:document WHERE cmis:name     LIKE 'A\\%'"), null),
                "*[count(//doc)=0]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmis:name FROM cmis:document WHERE cmis:name     LIKE 'BB_'"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmis:name FROM cmis:document WHERE cmis:name     LIKE 'BB\\_'"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmis:name FROM cmis:document WHERE cmis:name     LIKE 'B__'"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmis:name FROM cmis:document WHERE cmis:name     LIKE 'B_\\_'"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmis:name FROM cmis:document WHERE cmis:name     LIKE 'B\\_\\_'"), null),
                "*[count(//doc)=0]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmis:name FROM cmis:document WHERE cmis:name     LIKE 'CC\\\\'"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmis:name FROM cmis:document WHERE cmis:name     LIKE 'DD\\''"), null),
                "*[count(//doc)=1]");



        /******* checkDateFormatting  ******/

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cm:lockable L WHERE L.cm:expiryDate =  TIMESTAMP '2012-12-12T12:12:12.012Z'"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cm:lockable L WHERE L.cm:expiryDate =  TIMESTAMP '2012-012-12T12:12:12.012Z'"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cm:lockable L WHERE L.cm:expiryDate =  TIMESTAMP '2012-2-12T12:12:12.012Z'"), null),
                "*[count(//doc)=0]");

        /********** checkAspectJoin  **********/

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "select o.*, t.* from ( cm:ownable o join cm:titled t on o.cmis:objectId = t.cmis:objectId JOIN cmis:document AS D ON D.cmis:objectId = o.cmis:objectId  ) where o.cm:owner = 'andy' and t.cm:title = 'Alfresco tutorial' and CONTAINS(D, '\\'jumped\\'') and D.cmis:contentStreamLength <> 2"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cm:ownable"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cm:ownable where cm:owner = 'andy'"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cm:ownable where cm:owner = 'bob'"), null),
                "*[count(//doc)=0]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT D.*, O.* FROM cmis:document AS D JOIN cm:ownable AS O ON D.cmis:objectId = O.cmis:objectId"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT D.*, O.*, T.* FROM cmis:document AS D JOIN cm:ownable AS O ON D.cmis:objectId = O.cmis:objectId JOIN cm:titled AS T ON T.cmis:objectId = D.cmis:objectId"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT D.*, O.* FROM cm:ownable O JOIN cmis:document D ON D.cmis:objectId = O.cmis:objectId"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT D.*, F.* FROM cmis:folder F JOIN cmis:document D ON D.cmis:objectId = F.cmis:objectId"), null),
                "*[count(//doc)=0]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT O.*, T.* FROM cm:ownable O JOIN cm:titled T ON O.cmis:objectId = T.cmis:objectId"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "select o.*, t.* from cm:ownable o join cm:titled t on o.cmis:objectId = t.cmis:objectId"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "sElEcT o.*, T.* fRoM cm:ownable o JoIn cm:titled T oN o.cmis:objectId = T.cmis:objectId"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "select o.*, t.* from ( cm:ownable o join cm:titled t on o.cmis:objectId = t.cmis:objectId )"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "select o.*, t.* from ( cm:ownable o join cm:titled t on o.cmis:objectId = t.cmis:objectId  JOIN cmis:document AS D ON D.cmis:objectId = o.cmis:objectId  )"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "select o.*, t.* from ( cm:ownable o join cm:titled t on o.cmis:objectId = t.cmis:objectId JOIN cmis:document AS D ON D.cmis:objectId = o.cmis:objectId ) where o.cm:owner = 'andy' and t.cm:title = 'Alfresco tutorial' and CONTAINS(D, '\\'jumped\\'') and D.cmis:contentStreamLength <> 2"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "select o.*, t.* from ( cm:ownable o join cm:titled t on o.cmis:objectId = t.cmis:objectId JOIN cmis:document AS D ON D.cmis:objectId = o.cmis:objectId ) where o.cm:owner = 'andy' and t.cm:title = 'Alfresco tutorial' and CONTAINS(D, 'jumped') and D.cmis:contentStreamLength <> 2"), null),
                "*[count(//doc)=1]");

        /********* check FTS *************/

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT SCORE()as ONE, SCORE()as TWO, D.* FROM cmis:document D WHERE CONTAINS('\\'zebra\\'')"), null),
                "*[count(//doc)=10]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                "SELECT * FROM cmis:document WHERE CONTAINS('\\'zebra\\'')"), null),
                "*[count(//doc)=10]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmis:document WHERE CONTAINS('\\'quick\\'')"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmis:document WHERE CONTAINS('\\'quick\\'')"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmis:document D WHERE CONTAINS(D, 'cmis:name:\\'Tutorial\\'')"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmis:name as BOO FROM cmis:document D WHERE CONTAINS('BOO:\\'Tutorial\\'')"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmis:document D WHERE CONTAINS('TEXT:\\'zebra\\'')"), null),
                "*[count(//doc)=10]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmis:document D WHERE CONTAINS('d:content:\\'zebra\\'')"), null),
                "*[count(//doc)=10]");

        /******* checkFTSConnectives ********/

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmis:document where contains('\\'two\\' OR \\'zebra\\'')"), null),
                "*[count(//doc)=10]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmis:document where contains('\\'two\\' or \\'zebra\\'')"), null),
                "*[count(//doc)=10]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmis:document where contains('\\'two\\' \\'zebra\\'')"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmis:document where contains('\\'two\\' and \\'zebra\\'')"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmis:document where contains('\\'two\\' or \\'zebra\\'')"), null),
                "*[count(//doc)=10]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmis:document where contains('\\'two\\'  \\'zebra\\'')"), null),
                "*[count(//doc)=1]");


        /******** Load record ************/

        addTypeTestData(testCMISFolder00NodeRef,
                testCMISRootNodeRef,
                testCMISBaseFolderNodeRef,
                testCMISBaseFolderQName,
                testCMISFolder00QName,
                testCMISDate00);


        /******* check_D_text *******/

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmis:document"), null),
                "*[count(//doc)=12]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleTextBoth = 'Un tokenised'"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleTextBoth <> 'tokenised'"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleTextBoth LIKE 'U_ to%sed'"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleTextBoth NOT LIKE 't__eni%'"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleTextBoth IN ('Un tokenised', 'Monkey')"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleTextBoth NOT IN ('Un tokenized')"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleTextBoth < 'tokenised'"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleTextBoth < 'Un tokenised'"), null),
                "*[count(//doc)=0]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleTextBoth < 'V'"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleTextBoth < 'U'"), null),
                "*[count(//doc)=0]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleTextBoth <= 'tokenised'"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleTextBoth <= 'Un tokenised'"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleTextBoth <= 'V'"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleTextBoth <= 'U'"), null),
                "*[count(//doc)=0]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleTextBoth > 'tokenised'"), null),
                "*[count(//doc)=0]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleTextBoth > 'Un tokenised'"), null),
                "*[count(//doc)=0]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleTextBoth > 'V'"), null),
                "*[count(//doc)=0]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleTextBoth > 'U'"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleTextBoth >= 'tokenised'"), null),
                "*[count(//doc)=0]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleTextBoth >= 'Un tokenised'"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleTextBoth >= 'V'"), null),
                "*[count(//doc)=0]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleTextBoth >= 'U'"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleTextUntokenised = 'Un tokenised'"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleTextUntokenised <> 'tokenised'"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleTextUntokenised LIKE 'U_ to%sed'"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleTextUntokenised NOT LIKE 't__eni%'"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleTextUntokenised IN ('Un tokenised', 'Monkey')"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleTextUntokenised NOT IN ('Un tokenized')"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleTextUntokenised < 'tokenised'"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleTextUntokenised < 'Un tokenised'"), null),
                "*[count(//doc)=0]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleTextUntokenised < 'V'"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleTextUntokenised < 'U'"), null),
                "*[count(//doc)=0]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleTextUntokenised <= 'tokenised'"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleTextUntokenised <= 'Un tokenised'"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleTextUntokenised <= 'V'"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleTextUntokenised <= 'U'"), null),
                "*[count(//doc)=0]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleTextUntokenised > 'tokenised'"), null),
                "*[count(//doc)=0]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleTextUntokenised > 'Un tokenised'"), null),
                "*[count(//doc)=0]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleTextUntokenised > 'V'"), null),
                "*[count(//doc)=0]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleTextUntokenised > 'U'"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleTextUntokenised >= 'tokenised'"), null),
                "*[count(//doc)=0]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleTextUntokenised >= 'Un tokenised'"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleTextUntokenised >= 'V'"), null),
                "*[count(//doc)=0]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleTextUntokenised >= 'U'"), null),
                "*[count(//doc)=1]");


        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleTextTokenised = 'tokenised'"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleTextTokenised <> 'tokenized'"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleTextTokenised LIKE 'to%sed'"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleTextTokenised NOT LIKE 'Ut__eniz%'"), null),
                "*[count(//doc)=1]");


        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleTextTokenised IN ('tokenised', 'Monkey')"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleTextTokenised NOT IN ('tokenized')"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT T.cmistest:singleTextBoth as alias FROM cmistest:extendedContent as T WHERE alias = 'Un tokenised'"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT T.cmistest:singleTextBoth as alias FROM cmistest:extendedContent as T WHERE alias <> 'tokenised'"), null),
                "*[count(//doc)=1]");


        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT T.cmistest:singleTextBoth as alias FROM cmistest:extendedContent as T WHERE alias LIKE 'U_ to%sed'"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT T.cmistest:singleTextBoth as alias FROM cmistest:extendedContent as T WHERE alias NOT LIKE 't__eni%'"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT T.cmistest:singleTextBoth as alias FROM cmistest:extendedContent as T WHERE alias IN ('Un tokenised', 'Monkey')"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT T.cmistest:singleTextBoth as alias FROM cmistest:extendedContent as T WHERE alias NOT IN ('Un tokenized')"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                "SELECT T.cmistest:singleTextUntokenised as alias FROM cmistest:extendedContent as T WHERE alias = 'Un tokenised'"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT T.cmistest:singleTextUntokenised as alias FROM cmistest:extendedContent as T WHERE alias <> 'tokenised'"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT T.cmistest:singleTextUntokenised as alias FROM cmistest:extendedContent as T WHERE alias LIKE 'U_ to%sed'"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT T.cmistest:singleTextUntokenised as alias FROM cmistest:extendedContent as T WHERE alias NOT LIKE 't__eni%'"), null),
                "*[count(//doc)=1]");


        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT T.cmistest:singleTextUntokenised as alias FROM cmistest:extendedContent as T WHERE alias IN ('Un tokenised', 'Monkey')"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT T.cmistest:singleTextUntokenised as alias FROM cmistest:extendedContent as T WHERE alias NOT IN ('Un tokenized')"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmistest:singleTextTokenised as alias FROM cmistest:extendedContent WHERE alias = 'tokenised'"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmistest:singleTextTokenised as alias FROM cmistest:extendedContent WHERE alias <> 'tokenized'"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmistest:singleTextTokenised as alias FROM cmistest:extendedContent WHERE alias LIKE 'to%sed'"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmistest:singleTextTokenised as alias FROM cmistest:extendedContent WHERE alias NOT LIKE 'Ut__eniz%'"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmistest:singleTextTokenised as alias FROM cmistest:extendedContent WHERE alias IN ('tokenised', 'Monkey')"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmistest:singleTextTokenised as alias FROM cmistest:extendedContent WHERE alias NOT IN ('tokenized')"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE 'Un tokenised' =  ANY cmistest:multipleTextBoth "), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE ANY cmistest:multipleTextBoth IN ('Un tokenised', 'Monkey')"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE ANY cmistest:multipleTextBoth NOT IN ('Un tokenized')"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE 'Un tokenised' =  ANY cmistest:multipleTextUntokenised "), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE ANY cmistest:multipleTextUntokenised IN ('Un tokenised', 'Monkey')"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE ANY cmistest:multipleTextUntokenised NOT IN ('Un tokenized')"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE 'tokenised' =  ANY cmistest:multipleTextTokenised "), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE ANY cmistest:multipleTextTokenised IN ('tokenised', 'Monkey')"), null),
                "*[count(//doc)=1]");


        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE ANY cmistest:multipleTextTokenised NOT IN ('tokenized')"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmistest:multipleTextBoth as alias FROM cmistest:extendedContent WHERE 'Un tokenised' =  ANY alias "), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmistest:multipleTextBoth as alias FROM cmistest:extendedContent WHERE ANY alias IN ('Un tokenised', 'Monkey')"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmistest:multipleTextBoth as alias FROM cmistest:extendedContent WHERE ANY alias NOT IN ('Un tokenized')"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmistest:multipleTextUntokenised alias FROM cmistest:extendedContent WHERE 'Un tokenised' =  ANY alias "), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmistest:multipleTextUntokenised alias FROM cmistest:extendedContent WHERE ANY alias IN ('Un tokenised', 'Monkey')"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmistest:multipleTextUntokenised alias FROM cmistest:extendedContent WHERE ANY alias NOT IN ('Un tokenized')"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                "SELECT T.cmistest:multipleTextTokenised alias FROM cmistest:extendedContent T WHERE 'tokenised' =  ANY alias "), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT T.cmistest:multipleTextTokenised alias FROM cmistest:extendedContent T WHERE ANY alias IN ('tokenised', 'Monkey')"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT T.cmistest:multipleTextTokenised alias FROM cmistest:extendedContent T WHERE ANY alias NOT IN ('tokenized')"), null),
                "*[count(//doc)=1]");


        /******** check_D_mltext     **********/

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleMLTextBoth = 'AAAA BBBB'"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleMLTextBoth = 'AAAA'"), null),
                "*[count(//doc)=0]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleMLTextBoth = '%AAAA'"), null),
                "*[count(//doc)=0]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleMLTextBoth = '%AAA'"), null),
                "*[count(//doc)=0]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleMLTextBoth = 'BBBB'"), null),
                "*[count(//doc)=0]");


        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleMLTextBoth = 'CCCC DDDD'"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleMLTextBoth <> 'EEEE FFFF'"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleMLTextBoth LIKE 'AAA_ B%'"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleMLTextBoth LIKE 'CCC_ D%'"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleMLTextBoth NOT LIKE 'B%'"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleMLTextBoth NOT LIKE 'D%'"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleMLTextBoth IN ('AAAA BBBB', 'Monkey')"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleMLTextBoth IN ('CCCC DDDD', 'Monkey')"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleMLTextBoth NOT IN ('EEEE FFFF')"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleMLTextUntokenised = 'AAAA BBBB'"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleMLTextUntokenised = 'CCCC DDDD'"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleMLTextUntokenised <> 'EEEE FFFF'"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleMLTextUntokenised LIKE 'AAA_ B%'"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleMLTextUntokenised LIKE 'CCC_ D%'"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleMLTextUntokenised NOT LIKE 'B%'"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleMLTextUntokenised NOT LIKE 'D%'"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleMLTextUntokenised IN ('AAAA BBBB', 'Monkey')"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleMLTextUntokenised IN ('CCCC DDDD', 'Monkey')"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleMLTextUntokenised NOT IN ('EEEE FFFF')"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleMLTextTokenised = 'AAAA'"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleMLTextTokenised = 'BBBB'"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleMLTextTokenised = 'CCCC'"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleMLTextTokenised = 'DDDD'"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleMLTextTokenised <> 'EEEE'"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleMLTextTokenised LIKE 'A%'"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleMLTextTokenised LIKE '_B__'"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleMLTextTokenised LIKE '%C'"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleMLTextTokenised LIKE 'D%D'"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleMLTextTokenised NOT LIKE 'CCCC_'"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleMLTextTokenised IN ('AAAA', 'Monkey')"), null),
                "*[count(//doc)=1]");


        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleMLTextTokenised IN ('BBBB', 'Monkey')"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleMLTextTokenised IN ('CCCC', 'Monkey')"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleMLTextTokenised IN ('DDDD', 'Monkey')"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleMLTextTokenised NOT IN ('EEEE')"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmistest:singleMLTextBoth as alias FROM cmistest:extendedContent WHERE alias = 'AAAA BBBB'"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmistest:singleMLTextBoth as alias FROM cmistest:extendedContent WHERE alias = 'AAAA'"), null),
                "*[count(//doc)=0]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmistest:singleMLTextBoth as alias FROM cmistest:extendedContent WHERE alias = 'BBBB'"), null),
                "*[count(//doc)=0]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                "SELECT cmistest:singleMLTextBoth as alias FROM cmistest:extendedContent WHERE alias = 'CCCC DDDD'"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmistest:singleMLTextBoth as alias FROM cmistest:extendedContent WHERE alias <> 'EEEE FFFF'"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmistest:singleMLTextBoth as alias FROM cmistest:extendedContent WHERE alias LIKE 'AAA_ B%'"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmistest:singleMLTextBoth as alias FROM cmistest:extendedContent WHERE alias LIKE 'CCC_ D%'"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmistest:singleMLTextBoth as alias FROM cmistest:extendedContent WHERE alias NOT LIKE 'B%'"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmistest:singleMLTextBoth as alias FROM cmistest:extendedContent WHERE alias NOT LIKE 'D%'"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmistest:singleMLTextBoth as alias FROM cmistest:extendedContent WHERE alias IN ('AAAA BBBB', 'Monkey')"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmistest:singleMLTextBoth as alias FROM cmistest:extendedContent WHERE alias IN ('CCCC DDDD', 'Monkey')"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmistest:singleMLTextBoth as alias FROM cmistest:extendedContent WHERE alias NOT IN ('EEEE FFFF')"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmistest:singleMLTextUntokenised as alias FROM cmistest:extendedContent WHERE alias = 'AAAA BBBB'"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmistest:singleMLTextUntokenised as alias FROM cmistest:extendedContent WHERE alias = 'CCCC DDDD'"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmistest:singleMLTextUntokenised as alias FROM cmistest:extendedContent WHERE alias <> 'EEEE FFFF'"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmistest:singleMLTextUntokenised as alias FROM cmistest:extendedContent WHERE alias LIKE 'AAA_ B%'"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmistest:singleMLTextUntokenised as alias FROM cmistest:extendedContent WHERE alias LIKE 'CCC_ D%'"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmistest:singleMLTextUntokenised as alias FROM cmistest:extendedContent WHERE alias NOT LIKE 'B%'"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmistest:singleMLTextUntokenised as alias FROM cmistest:extendedContent WHERE alias NOT LIKE 'D%'"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmistest:singleMLTextUntokenised as alias FROM cmistest:extendedContent WHERE alias IN ('AAAA BBBB', 'Monkey')"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmistest:singleMLTextUntokenised as alias FROM cmistest:extendedContent WHERE alias IN ('CCCC DDDD', 'Monkey')"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmistest:singleMLTextUntokenised as alias FROM cmistest:extendedContent WHERE alias NOT IN ('EEEE FFFF')"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmistest:singleMLTextTokenised as alias FROM cmistest:extendedContent WHERE alias = 'AAAA'"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmistest:singleMLTextTokenised as alias FROM cmistest:extendedContent WHERE alias = 'BBBB'"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmistest:singleMLTextTokenised as alias FROM cmistest:extendedContent WHERE alias = 'CCCC'"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmistest:singleMLTextTokenised as alias FROM cmistest:extendedContent WHERE alias = 'DDDD'"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmistest:singleMLTextTokenised as alias FROM cmistest:extendedContent WHERE alias <> 'EEEE'"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmistest:singleMLTextTokenised as alias FROM cmistest:extendedContent WHERE alias LIKE 'A%'"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmistest:singleMLTextTokenised as alias FROM cmistest:extendedContent WHERE alias LIKE '_B__'"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmistest:singleMLTextTokenised as alias FROM cmistest:extendedContent WHERE alias LIKE '%C'"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmistest:singleMLTextTokenised as alias FROM cmistest:extendedContent WHERE alias LIKE 'D%D'"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmistest:singleMLTextTokenised as alias FROM cmistest:extendedContent WHERE alias NOT LIKE 'CCCC_'"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmistest:singleMLTextTokenised as alias FROM cmistest:extendedContent WHERE alias IN ('AAAA', 'Monkey')"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmistest:singleMLTextTokenised as alias FROM cmistest:extendedContent WHERE alias IN ('BBBB', 'Monkey')"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmistest:singleMLTextTokenised as alias FROM cmistest:extendedContent WHERE alias IN ('CCCC', 'Monkey')"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmistest:singleMLTextTokenised as alias FROM cmistest:extendedContent WHERE alias IN ('DDDD', 'Monkey')"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmistest:singleMLTextTokenised as alias FROM cmistest:extendedContent WHERE alias NOT IN ('EEEE')"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE 'AAAA BBBB' =  ANY cmistest:multipleMLTextBoth "), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE 'CCCC DDDD' =  ANY cmistest:multipleMLTextBoth "), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE ANY cmistest:multipleMLTextBoth IN ('AAAA BBBB', 'Monkey')"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE ANY cmistest:multipleMLTextBoth IN ('CCCC DDDD', 'Monkey')"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE ANY cmistest:multipleMLTextBoth NOT IN ('EEEE FFFF')"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE 'AAAA BBBB' =  ANY cmistest:multipleMLTextUntokenised "), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE 'CCCC DDDD' =  ANY cmistest:multipleMLTextUntokenised "), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE ANY cmistest:multipleMLTextUntokenised IN ('AAAA BBBB', 'Monkey')"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE ANY cmistest:multipleMLTextUntokenised IN ('CCCC DDDD', 'Monkey')"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE ANY cmistest:multipleMLTextUntokenised NOT IN ('EEEE FFFF')"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE 'AAAA' =  ANY cmistest:multipleMLTextTokenised "), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE 'BBBB' =  ANY cmistest:multipleMLTextTokenised "), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE 'CCCC' =  ANY cmistest:multipleMLTextTokenised "), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE 'DDDD' =  ANY cmistest:multipleMLTextTokenised "), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE ANY cmistest:multipleMLTextTokenised IN ('AAAA', 'Monkey')"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE ANY cmistest:multipleMLTextTokenised IN ('BBBB', 'Monkey')"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE ANY cmistest:multipleMLTextTokenised IN ('CCCC', 'Monkey')"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE ANY cmistest:multipleMLTextTokenised IN ('DDDD', 'Monkey')"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE ANY cmistest:multipleMLTextTokenised NOT IN ('EEEE')"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmistest:multipleMLTextBoth alias FROM cmistest:extendedContent WHERE 'AAAA BBBB' =  ANY alias "), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmistest:multipleMLTextBoth alias FROM cmistest:extendedContent WHERE 'CCCC DDDD' =  ANY alias "), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmistest:multipleMLTextBoth alias FROM cmistest:extendedContent WHERE ANY alias IN ('AAAA BBBB', 'Monkey')"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmistest:multipleMLTextBoth alias FROM cmistest:extendedContent WHERE ANY alias IN ('CCCC DDDD', 'Monkey')"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmistest:multipleMLTextBoth alias FROM cmistest:extendedContent WHERE ANY alias NOT IN ('EEEE FFFF')"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmistest:multipleMLTextUntokenised alias FROM cmistest:extendedContent WHERE 'AAAA BBBB' =  ANY alias "), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmistest:multipleMLTextUntokenised alias FROM cmistest:extendedContent WHERE 'CCCC DDDD' =  ANY alias "), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmistest:multipleMLTextUntokenised alias FROM cmistest:extendedContent WHERE ANY alias IN ('AAAA BBBB', 'Monkey')"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmistest:multipleMLTextUntokenised alias FROM cmistest:extendedContent WHERE ANY alias IN ('CCCC DDDD', 'Monkey')"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmistest:multipleMLTextUntokenised alias FROM cmistest:extendedContent WHERE ANY alias NOT IN ('EEEE FFFF')"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmistest:multipleMLTextTokenised alias FROM cmistest:extendedContent WHERE 'AAAA' =  ANY alias "), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmistest:multipleMLTextTokenised alias FROM cmistest:extendedContent WHERE 'BBBB' =  ANY alias "), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmistest:multipleMLTextTokenised alias FROM cmistest:extendedContent WHERE 'CCCC' =  ANY alias "), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmistest:multipleMLTextTokenised alias FROM cmistest:extendedContent WHERE 'DDDD' =  ANY alias "), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmistest:multipleMLTextTokenised alias FROM cmistest:extendedContent WHERE ANY alias IN ('AAAA', 'Monkey')"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmistest:multipleMLTextTokenised alias FROM cmistest:extendedContent WHERE ANY alias IN ('BBBB', 'Monkey')"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmistest:multipleMLTextTokenised alias FROM cmistest:extendedContent WHERE ANY alias IN ('CCCC', 'Monkey')"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmistest:multipleMLTextTokenised alias FROM cmistest:extendedContent WHERE ANY alias IN ('DDDD', 'Monkey')"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmistest:multipleMLTextTokenised alias FROM cmistest:extendedContent WHERE ANY alias NOT IN ('EEEE')"), null),
                "*[count(//doc)=1]");
        
        /******** check_D_mltext     **********/
    }
}