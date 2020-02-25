/*
 * Copyright (C) 2020 Alfresco Software Limited.
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