package org.alfresco.solr;

import static java.util.Optional.ofNullable;

import org.apache.lucene.index.IndexableField;
import org.apache.solr.schema.SchemaField;
import org.apache.solr.schema.StrField;

import java.util.List;

/**
 * A {@link StrField} subclass which removes the locale language marker at the beginning of its value.
 */
public class StripLocaleStrField extends StrField
{
    @Override
    public List<IndexableField> createFields(SchemaField field, Object value, float boost)
    {
        Object newValue =
                ofNullable(value).map(String.class::cast)
                        .map(v -> v.replaceFirst("\\x{0000}.*\\x{0000}", ""))
                        .orElse(null);
        return super.createFields(field, newValue, boost);
    }
}