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
import org.alfresco.solr.AbstractAlfrescoSolrTests;
import org.apache.lucene.util.LuceneTestCase;
import org.apache.solr.SolrTestCaseJ4;
import org.junit.Test;


@LuceneTestCase.SuppressCodecs({"Appending","Lucene3x","Lucene40","Lucene41","Lucene42","Lucene43", "Lucene44", "Lucene45","Lucene46","Lucene47","Lucene48","Lucene49"})
@SolrTestCaseJ4.SuppressSSL
public class TestAlfrescoCMISQParserPlugin extends AbstractAlfrescoSolrTests implements QueryConstants 
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
                        + testBaseFolderNodeRef + "'"), null),
                "*[count(//doc)=4]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q", "SELECT cmis:parentId FROM cmis:folder WHERE cmis:parentId <> '"
                        + testBaseFolderNodeRef + "'"), null),
                "*[count(//doc)=7]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q", "SELECT cmis:parentId FROM cmis:folder WHERE cmis:parentId IN     ('"
                        + testBaseFolderNodeRef + "')"), null),
                "*[count(//doc)=4]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q", "SELECT cmis:parentId FROM cmis:folder WHERE cmis:parentId NOT IN ('"
                        + testBaseFolderNodeRef + "')"), null),
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

    }


}