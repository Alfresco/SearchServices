
package org.alfresco.solr.component.spellcheck;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.solr.common.params.ShardParams;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.common.util.SimpleOrderedMap;
import org.apache.solr.handler.component.ResponseBuilder;
import org.apache.solr.handler.component.SearchComponent;
import org.apache.solr.response.SolrQueryResponse;
import org.apache.solr.spelling.SpellingResult;

/**
 * @author Joel Bernstein
 * @since 5.2
 */
public class AlfrescoSpellCheckBackCompatComponent extends SearchComponent
{
    public void prepare(ResponseBuilder responseBuilder) {

    }

    public void process(ResponseBuilder responseBuilder) {

        NamedList response = responseBuilder.rsp.getValues();
        NamedList spellcheck = (NamedList)response.get("spellcheck");
        NamedList spellcheckExtras = (NamedList)response.get("spellcheck-extras");
        NamedList collations = (NamedList)spellcheck.get("collations");
        NamedList suggest = (NamedList)spellcheck.get("suggest");

        NamedList collationList = collations != null ? collations : suggest;

        String shards = responseBuilder.req.getParams().get("shards");
        boolean distributed = shards != null ? true : false;

        if(distributed) {
            for(int i=0; i<collationList.size(); i++) {
                if("collation".equals(collationList.getName(i))) {
                    NamedList collation = (NamedList) collationList.getVal(i);
                    String collationQuery = (String) collation.get("collationQuery");
                    String collationQueryString = (String) spellcheckExtras.get(collationQuery);
                    collation.add("collationQueryString", collationQueryString);
                    suggest.add("collation", collation);
                }
            }
            spellcheck.remove("collations");
        } else {
            NamedList spellCheckExtras = new NamedList();
            for(int i=0; i<collationList.size(); i++) {
                if("collation".equals(collationList.getName(i))) {
                    NamedList collation = (NamedList) collationList.getVal(i);
                    String collationQuery = (String) collation.get("collationQuery");
                    String collationQueryString = (String) collation.get("collationQueryString");
                    spellCheckExtras.add(collationQuery, collationQueryString);
                }
            }
            response.add("spellcheck-extras", spellCheckExtras);
        }
    }

    public String getDescription() {
        return null;
    }
}
