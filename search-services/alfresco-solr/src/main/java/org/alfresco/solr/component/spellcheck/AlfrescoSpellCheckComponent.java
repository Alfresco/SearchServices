
package org.alfresco.solr.component.spellcheck;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.solr.common.params.ShardParams;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.common.util.SimpleOrderedMap;
import org.apache.solr.handler.component.ResponseBuilder;
import org.apache.solr.handler.component.SpellCheckComponent;
import org.apache.solr.spelling.SpellingResult;

/**
 * @author Jamal Kaabi-Mofrad
 * @since 5.0
 */
public class AlfrescoSpellCheckComponent extends SpellCheckComponent
{

    /**
     * <b>Disclaimer</b>: The code copied from the super class (
     * {@link org.apache.solr.handler.component.SpellCheckComponent}) but only
     * replaced the collator with the AlfrescoSpellCheckCollator
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    protected void addCollationsToResponse(SolrParams params, SpellingResult spellingResult, ResponseBuilder rb,
                String q, NamedList response, boolean suggestionsMayOverlap)
    {
        int maxCollations = params.getInt(SPELLCHECK_MAX_COLLATIONS, 1);
        int maxCollationTries = params.getInt(SPELLCHECK_MAX_COLLATION_TRIES, 0);
        int maxCollationEvaluations = params.getInt(SPELLCHECK_MAX_COLLATION_EVALUATIONS, 10000);
        boolean collationExtendedResults = params.getBool(SPELLCHECK_COLLATE_EXTENDED_RESULTS, false);
        int maxCollationCollectDocs = params.getInt(SPELLCHECK_COLLATE_MAX_COLLECT_DOCS, 0);
        // If not reporting hits counts, don't bother collecting more than 1 document per try.
        if (!collationExtendedResults)
        {
            maxCollationCollectDocs = 1;
        }
        boolean shard = params.getBool(ShardParams.IS_SHARD, false);
        AlfrescoSpellCheckCollator collator = new AlfrescoSpellCheckCollator();
        collator.setMaxCollations(maxCollations);
        collator.setMaxCollationTries(maxCollationTries);
        collator.setMaxCollationEvaluations(maxCollationEvaluations);
        collator.setSuggestionsMayOverlap(suggestionsMayOverlap);
        collator.setDocCollectionLimit(maxCollationCollectDocs);

        List<AlfrescoSpellCheckCollation> collations = collator.collate(spellingResult, q, rb);
        // by sorting here we guarantee a non-distributed request returns all
        // results in the same order as a distributed request would,
        // even in cases when the internal rank is the same.
        Collections.sort(collations);

        NamedList collationList = new NamedList();

        for (AlfrescoSpellCheckCollation collation : collations)
        {
            if (collationExtendedResults)
            {
                NamedList extendedResult = new SimpleOrderedMap();
                extendedResult.add("collationQuery", collation.getCollationQuery());
                extendedResult.add("hits", collation.getHits());
                extendedResult.add("misspellingsAndCorrections", collation.getMisspellingsAndCorrections());
                if (maxCollationTries > 0 && shard)
                {
                    extendedResult.add("collationInternalRank", collation.getInternalRank());
                }
                extendedResult.add("collationQueryString", collation.getCollationQueryString());
                collationList.add("collation", extendedResult);
            }
            else
            {
                collationList.add("collation", collation.getCollationQuery());
                if (maxCollationTries > 0 && shard)
                {
                   collationList.add("collationInternalRank", collation.getInternalRank());
                }
            }
        }

        //Support Solr 4 output format
        NamedList suggestions = (NamedList)response.get("suggestions");
        suggestions.addAll(collationList);

        //Support Solr distributed merge format.
        response.add("collations", collationList);
    }
}
