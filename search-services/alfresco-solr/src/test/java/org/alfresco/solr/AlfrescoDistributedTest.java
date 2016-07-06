
package org.alfresco.solr;

import org.alfresco.distributed.AbstractAlfrescoDistributedTest;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.carrotsearch.randomizedtesting.RandomizedRunner;
@RunWith(RandomizedRunner.class)
public class AlfrescoDistributedTest extends AbstractAlfrescoDistributedTest
{
    
    @Before
    public void load() throws Exception
    {
        del("*:*");
        index_specific(0, "id", "1", "content@s___t@{http://www.alfresco.org/model/content/1.0}content", "YYYY");
        index_specific(0, "id", "2", "content@s___t@{http://www.alfresco.org/model/content/1.0}content", "YYYY");
//        index_specific(1, "id", "3", "content@s___t@{http://www.alfresco.org/model/content/1.0}content", "YYYY");
//        index_specific(1, "id", "4", "content@s___t@{http://www.alfresco.org/model/content/1.0}content", "YYYY");
        commit();
    }
    @Test
    public void test() throws Exception
    {
        query("{\"locales\":[\"en\"], \"templates\": [{\"name\":\"t1\", \"template\":\"%cm:content\"}]}",
                params("q", "t1:YYYY", "qt", "/afts", "shards.qt","/afts","start", "0", "rows", "6", "sort", "id asc"));
    }
}
