package org.alfresco.test.search.functional.searchServices.cmis;

import org.alfresco.utility.Utility;
import org.alfresco.utility.data.CustomObjectTypeProperties;
import org.alfresco.utility.data.provider.XMLDataConfig;
import org.alfresco.utility.data.provider.XMLTestDataProvider;
import org.alfresco.utility.model.FileModel;
import org.alfresco.utility.model.FolderModel;
import org.alfresco.utility.model.QueryModel;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class SolrSearchByPropertyTests extends AbstractCmisE2ETest
{
    private FolderModel guestf, tesf, restf, testtttf, testf, testf1, testf2, testf3, testf4;
    private FileModel guestc, restc, tesc, testtttc, testc, testc1, testc2, testc3;

    @BeforeClass(alwaysRun = true)
    public void dataPreparation() throws Exception
    {
        dataContent.usingAdmin().deployContentModel("model/tas-model.xml");

        // Folders
        guestf = new FolderModel("guestf");
        tesf = new FolderModel("tesf");

        // SubFolders
        restf = new FolderModel("restf");
        testtttf = new FolderModel("testtttf");
        testf = new FolderModel("testf");
        testf1 = new FolderModel("testf1");
        testf2 = new FolderModel("testf2");
        testf3 = new FolderModel("testf3");
        testf4 = new FolderModel("testf4");

        // Files
        guestc = new FileModel("guestc.txt");
        restc = new FileModel("restc.txt");
        tesc = new FileModel("tesc.txt");
        testtttc = new FileModel("testtttc.txt");
        testc = new FileModel("testc.txt");
        testc1 = new FileModel("testc1.txt");
        testc2 = new FileModel("testc2.txt");
        testc3 = new FileModel("testc3.txt");

        // Sites
        dataContent.usingUser(testUser).usingSite(testSite).createCustomContent(guestf, "F:tas:folder",
                new CustomObjectTypeProperties().addProperty("tas:TextPropertyF", "guestf text")
                        .addProperty("tas:IntPropertyF", 222));
        dataContent.usingUser(testUser).usingSite(testSite).createCustomContent(tesf, "F:tas:folder",
                new CustomObjectTypeProperties().addProperty("tas:TextPropertyF", "tesf text")
                        .addProperty("tas:IntPropertyF", 224));

        // Sites >> Folders
        dataContent.usingUser(testUser).usingResource(guestf).createCustomContent(restf, "F:tas:folder",
                new CustomObjectTypeProperties().addProperty("tas:TextPropertyF", "restf text")
                        .addProperty("tas:IntPropertyF", 223));
        dataContent.usingUser(testUser).usingResource(guestf).createCustomContent(testtttf, "F:tas:folder",
                new CustomObjectTypeProperties().addProperty("tas:TextPropertyF", "testtttf text")
                        .addProperty("tas:IntPropertyF", 225));
        dataContent.usingUser(testUser).usingResource(guestf).createCustomContent(testf, "F:tas:folder",
                new CustomObjectTypeProperties().addProperty("tas:TextPropertyF", "testf text")
                        .addProperty("tas:IntPropertyF", 226));
        dataContent.usingUser(testUser).usingResource(guestf).createCustomContent(testf1, "F:tas:folder",
                new CustomObjectTypeProperties().addProperty("tas:TextPropertyF", "testf1 text")
                        .addProperty("tas:IntPropertyF", 2221));
        dataContent.usingUser(testUser).usingResource(guestf).createCustomContent(testf2, "F:tas:folder",
                new CustomObjectTypeProperties().addProperty("tas:TextPropertyF", "testf2 text")
                        .addProperty("tas:IntPropertyF", 2222));
        dataContent.usingUser(testUser).usingResource(guestf).createCustomContent(testf3, "F:tas:folder",
                new CustomObjectTypeProperties().addProperty("tas:TextPropertyF", "testf3 text")
                        .addProperty("tas:IntPropertyF", 2223));
        dataContent.usingUser(testUser).usingResource(guestf).createCustomContent(testf4, "F:tas:folder",
                new CustomObjectTypeProperties().addProperty("tas:TextPropertyF", "testf4 text")
                        .addProperty("tas:IntPropertyF", 2224));

        // Sites >> Files
        dataContent.usingUser(testUser).usingResource(tesf).createCustomContent(guestc, "D:tas:document",
                new CustomObjectTypeProperties().addProperty("tas:TextPropertyC", "guestc text")
                        .addProperty("tas:IntPropertyC", 222));
        dataContent.usingUser(testUser).usingResource(tesf).createCustomContent(restc, "D:tas:document",
                new CustomObjectTypeProperties().addProperty("tas:TextPropertyC", "restc text")
                        .addProperty("tas:IntPropertyC", 223));
        dataContent.usingUser(testUser).usingResource(tesf).createCustomContent(tesc, "D:tas:document",
                new CustomObjectTypeProperties().addProperty("tas:TextPropertyC", "tesc text")
                        .addProperty("tas:IntPropertyC", 224));
        dataContent.usingUser(testUser).usingResource(tesf).createCustomContent(testtttc, "D:tas:document",
                new CustomObjectTypeProperties().addProperty("tas:TextPropertyC", "testtttc text")
                        .addProperty("tas:IntPropertyC", 225));
        dataContent.usingUser(testUser).usingResource(tesf).createCustomContent(testc, "D:tas:document",
                new CustomObjectTypeProperties().addProperty("tas:TextPropertyC", "testc text")
                        .addProperty("tas:IntPropertyC", 226));
        dataContent.usingUser(testUser).usingResource(tesf).createCustomContent(testc1, "D:tas:document",
                new CustomObjectTypeProperties().addProperty("tas:TextPropertyC", "testc1 text")
                        .addProperty("tas:IntPropertyC", 2221));
        dataContent.usingUser(testUser).usingResource(tesf).createCustomContent(testc2, "D:tas:document",
                new CustomObjectTypeProperties().addProperty("tas:TextPropertyC", "testc2 text")
                        .addProperty("tas:IntPropertyC", 2222));
        dataContent.usingUser(testUser).usingResource(tesf).createCustomContent(testc3, "D:tas:document",
                new CustomObjectTypeProperties().addProperty("tas:TextPropertyC", "testc3 text")
                        .addProperty("tas:IntPropertyC", 2223));

        // wait for solr index
        Utility.waitToLoopTime(getSolrWaitTimeInSeconds());
    }

    @Test(dataProviderClass = XMLTestDataProvider.class, dataProvider = "getQueriesData")
    @XMLDataConfig(file = "src/test/resources/testdata/search-by-property.xml")
    public void executeSearchByProperty(QueryModel query) throws Exception
    {
        cmisApi.authenticateUser(testUser).withQuery(query.getValue()).assertResultsCount().equals(query.getResults());
    }
}
