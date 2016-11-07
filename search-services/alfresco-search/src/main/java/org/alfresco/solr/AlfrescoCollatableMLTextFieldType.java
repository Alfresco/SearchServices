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
package org.alfresco.solr;

import java.io.IOException;
import java.text.Collator;
import java.util.Locale;

import org.alfresco.service.cmr.repository.MLText;
import org.apache.lucene.index.BinaryDocValues;
import org.apache.lucene.index.DocValues;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.search.FieldComparator;
import org.apache.lucene.search.FieldComparatorSource;
import org.apache.lucene.search.LeafFieldComparator;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.SortField;
import org.apache.lucene.util.Bits;
import org.apache.lucene.util.BytesRef;
import org.apache.solr.schema.SchemaField;
import org.apache.solr.schema.StrField;
import org.springframework.extensions.surf.util.I18NUtil;

/**
 * @author Andy
 *
 */
public class AlfrescoCollatableMLTextFieldType extends StrField
{
    

    /* (non-Javadoc)
     * @see org.apache.solr.schema.StrField#getSortField(org.apache.solr.schema.SchemaField, boolean)
     */
    @Override
    public SortField getSortField(SchemaField field, boolean reverse)
    {
        return new SortField(field.getName(), new MLTextSortFieldComparatorSource(), reverse);
    }


    /* (non-Javadoc)
     * @see org.apache.solr.schema.StrField#marshalSortValue(java.lang.Object)
     */
    @Override
    public Object marshalSortValue(Object value)
    {
        return value;
    }


    /* (non-Javadoc)
     * @see org.apache.solr.schema.StrField#unmarshalSortValue(java.lang.Object)
     */
    @Override
    public Object unmarshalSortValue(Object value)
    {
        return value;
    }


    
    public static class MLTextSortFieldComparatorSource extends FieldComparatorSource
    {

        /*
         * (non-Javadoc)
         * @see org.apache.lucene.search.FieldComparatorSource#newComparator(java.lang.String, int, int, boolean)
         */
        @Override
        public FieldComparator<String> newComparator(String fieldname, int numHits, int sortPos, boolean reversed) throws IOException
        {
            return new MLTextSortFieldComparator(numHits, fieldname, I18NUtil.getLocale());
        }

    }


    /*
     * Adapted from org.apache.lucene.search.FieldComparator.TermValComparator<T>
     */
    public static final class MLTextSortFieldComparator extends FieldComparator<String> implements LeafFieldComparator
    {

        private final String[] values;

        private BinaryDocValues docTerms;
        
        private Bits docsWithField;

        private final String field;

        final Collator collator;

        private String bottom;
        
        private String top;

        Locale collatorLocale;

        MLTextSortFieldComparator(int numHits, String field, Locale collatorLocale)
        {
            values = new String[numHits];
            this.field = field;
            this.collatorLocale = collatorLocale;
            collator = Collator.getInstance(collatorLocale);
        }

        public int compare(int slot1, int slot2)
        {
            final String val1 = values[slot1];
            final String val2 = values[slot2];
            return compareValues(val1, val2);
        }

        public void setBottom(final int bottom)
        {
            this.bottom = values[bottom];
        }

        public int compareBottom(int doc)
        {
            final String comparableString = findBestValue(doc, docTerms.get(doc));
            return compareValues(bottom, comparableString);
           
        }

        public void copy(int slot, int doc)
        {
            values[slot] = findBestValue(doc, docTerms.get(doc));
        }

        public String value(int slot)
        {
            return values[slot];
        }

        private String findBestValue(int doc, BytesRef term)
        {
            if (term.length == 0 && docsWithField.get(doc) == false) {
                return null;
            }
            
            String withLocale = term.utf8ToString();
            
            // split strin into MLText object
            if (withLocale == null)
            {
                return withLocale;
            }
            else if (withLocale.startsWith("\u0000"))
            {
                MLText mlText = new MLText();
                String[] parts = withLocale.split("\u0000");
                for (int i = 0; (i + 2) <= parts.length; i += 3)
                {
                    Locale locale = null;
                    String[] localeParts = parts[i + 1].split("_");
                    if (localeParts.length == 1)
                    {
                        locale = new Locale(localeParts[0]);
                    }
                    else if (localeParts.length == 2)
                    {
                        locale = new Locale(localeParts[0], localeParts[1]);
                    }
                    else if (localeParts.length == 3)
                    {
                        locale = new Locale(localeParts[0], localeParts[1], localeParts[2]);
                    }
                    if (locale != null)
                    {
                        if (i + 2 == parts.length)
                        {
                            mlText.addValue(locale, "");
                        }
                        else
                        {
                            mlText.addValue(locale, parts[i + 2]);
                        }
                    }
                }
                return mlText.getClosestValue(collatorLocale);
            }
            else
            {
                return withLocale;
            }
        }

        /* (non-Javadoc)
         * @see org.apache.lucene.search.FieldComparator#setTopValue(java.lang.Object)
         */
        @Override
        public void setTopValue(String value)
        {
            this.top = value;
        }

        /* (non-Javadoc)
         * @see org.apache.lucene.search.FieldComparator#compareTop(int)
         */
        @Override
        public int compareTop(int doc) throws IOException
        {
            final String comparableString = findBestValue(doc, docTerms.get(doc));
            return compareValues(top, comparableString);
        }

        /* (non-Javadoc)
         * @see org.apache.lucene.search.FieldComparator#setNextReader(org.apache.lucene.index.AtomicReaderContext)
         */
        @Override
        public LeafFieldComparator getLeafComparator(LeafReaderContext context) throws IOException
        {
            docTerms = DocValues.getBinary(context.reader(), field);
            docsWithField = DocValues.getDocsWithField(context.reader(), field);
            if (docsWithField instanceof Bits.MatchAllBits) {
              docsWithField = null;
            }
            return this;
        }
        
        @Override
        public int compareValues(String val1, String val2) 
        {
            if (val1 == null)
            {
                if (val2 == null)
                {
                    return 0;
                }
                return -1;
            }
            else if (val2 == null)
            {
                return 1;
            }
            return collator.compare(val1, val2);
        }
        
        @Override
        public void setScorer(Scorer scorer) {}
    }

}
