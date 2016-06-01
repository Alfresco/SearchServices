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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.Weight;

/**
 * An extension to the Lucene query set. This query supports structured queries against paths. The field must have been
 * tokenised using the path tokeniser. This class manages linking together an ordered chain of absolute and relative
 * positional queries.
 * 
 * @author Andy Hind
 */
public class SolrPathQuery extends Query
{
    private String pathField = "PATH";

    private int unitSize = 2;

    private List<StructuredFieldPosition> pathStructuredFieldPositions = new ArrayList<StructuredFieldPosition>();

    private DictionaryService dictionaryService;

    private boolean repeats = false;

    /**
     * The base query
     * 
     * @param dictionaryService DictionaryService
     */

    public SolrPathQuery(DictionaryService dictionaryService)
    {
        super();
        this.dictionaryService = dictionaryService;
    }

    public void setQuery(List<StructuredFieldPosition> path)
    {
        pathStructuredFieldPositions.clear();
        if (path.size() % unitSize != 0)
        {
            throw new UnsupportedOperationException();
        }
        pathStructuredFieldPositions.addAll(path);
    }

    public void appendQuery(List<StructuredFieldPosition> sfps)
    {
        if (sfps.size() != unitSize)
        {
            throw new UnsupportedOperationException();
        }

        StructuredFieldPosition last = null;
        StructuredFieldPosition next = sfps.get(0);

        if (pathStructuredFieldPositions.size() > 0)
        {
            last = pathStructuredFieldPositions.get(pathStructuredFieldPositions.size() - 1);
        }

        if ((last != null) && next.linkParent() && !last.allowslinkingByParent())
        {
            return;
        }

        if ((last != null) && next.linkSelf() && !last.allowsLinkingBySelf())
        {
            return;
        }

        pathStructuredFieldPositions.addAll(sfps);
    }

    public String getPathField()
    {
        return pathField;
    }

    public void setPathField(String pathField)
    {
        this.pathField = pathField;
    }

    public Term getPathRootTerm()
    {
        return new Term(getPathField(), ";");
    }

    ArrayList<Term> getTerms()
    {
        ArrayList<Term> answer = new ArrayList<Term>(pathStructuredFieldPositions.size());
        for (StructuredFieldPosition sfp : pathStructuredFieldPositions)
        {
            if (sfp.getTermText() != null)
            {
                Term term = new Term(pathField, sfp.getTermText());
                answer.add(term);
            }
        }
        return answer;
    }
    
    /*
     * @see org.apache.lucene.search.Query#createWeight(org.apache.lucene.search.Searcher)
     */
    public Weight createWeight(IndexSearcher searcher, boolean needsScore)
    {
        return new StructuredFieldWeight();
    }

    /*
     * @see java.lang.Object#toString()
     */
    public String toString(String field)
    {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("PATH:");
        for(int i = 0; i < pathStructuredFieldPositions.size(); i+=2)
        {
            if(pathStructuredFieldPositions.get(i) instanceof AbsoluteStructuredFieldPosition)
            {
                stringBuilder.append("/");
                if((pathStructuredFieldPositions.get(i).getTermText() == null) && (pathStructuredFieldPositions.get(i+1).getTermText() == null))
                {
                    stringBuilder.append("*");
                }
                else
                {
                    stringBuilder.append("{");
                    stringBuilder.append(pathStructuredFieldPositions.get(i).getTermText() == null ? "*" : pathStructuredFieldPositions.get(i).getTermText());
                    stringBuilder.append("}:");
                    stringBuilder.append(pathStructuredFieldPositions.get(i+1).getTermText() == null ? "*" : pathStructuredFieldPositions.get(i+1).getTermText());
                    
                }
            }
            else if(pathStructuredFieldPositions.get(i) instanceof DescendantAndSelfStructuredFieldPosition)
            {
                stringBuilder.append("//");
            }
            else if(pathStructuredFieldPositions.get(i) instanceof RelativeStructuredFieldPosition)
            {
                stringBuilder.append("/");
                if((pathStructuredFieldPositions.get(i).getTermText() == null) && (pathStructuredFieldPositions.get(i+1).getTermText() == null))
                {
                    stringBuilder.append("*");
                }
                else
                {
                    stringBuilder.append(pathStructuredFieldPositions.get(i).getTermText() == null ? "*" : pathStructuredFieldPositions.get(i).getTermText());
                    stringBuilder.append(":");
                    stringBuilder.append(pathStructuredFieldPositions.get(i+1).getTermText() == null ? "*" : pathStructuredFieldPositions.get(i+1).getTermText());
                    
                }
            }
            else if(pathStructuredFieldPositions.get(i) instanceof SelfAxisStructuredFieldPosition)
            {
                stringBuilder.append(".");
            }
        }
        return stringBuilder.toString();
    }

    private class StructuredFieldWeight extends Weight
    {
        public StructuredFieldWeight()
        {
        	super(SolrPathQuery.this);
        }


        /* (non-Javadoc)
         * @see org.apache.lucene.search.Weight#explain(org.apache.lucene.index.AtomicReaderContext, int)
         */
        @Override
        public Explanation explain(LeafReaderContext context, int doc) throws IOException
        {
            throw new UnsupportedOperationException();
        }

        /* (non-Javadoc)
         * @see org.apache.lucene.search.Weight#getValueForNormalization()
         */
        @Override
        public float getValueForNormalization() throws IOException
        {
           return 1.0f;
        }

        /* (non-Javadoc)
         * @see org.apache.lucene.search.Weight#normalize(float, float)
         */
        @Override
        public void normalize(float norm, float topLevelBoost)
        {
          
            
        }

        /* (non-Javadoc)
         * @see org.apache.lucene.search.Weight#scorer(org.apache.lucene.index.AtomicReaderContext, org.apache.lucene.util.Bits)
         */
        @Override
        public Scorer scorer(LeafReaderContext context) throws IOException
        {
            return SolrPathScorer.createPathScorer(SolrPathQuery.this, context, this, dictionaryService, repeats);
        }


		@Override
		public void extractTerms(Set<Term> terms) {
			throw new UnsupportedOperationException();
			
		}
    }

    public void removeDescendantAndSelf()
    {
        while ((getLast() != null) && getLast().linkSelf())
        {
            removeLast();
            removeLast();
        }
    }

    private StructuredFieldPosition getLast()

    {
        if (pathStructuredFieldPositions.size() > 0)
        {
            return pathStructuredFieldPositions.get(pathStructuredFieldPositions.size() - 1);
        }
        else
        {
            return null;
        }
    }

    private void removeLast()
    {
        pathStructuredFieldPositions.remove(pathStructuredFieldPositions.size() - 1);
    }

    public boolean isEmpty()
    {
        return pathStructuredFieldPositions.size() == 0;
    }

    public List<StructuredFieldPosition> getPathStructuredFieldPositions()
    {
        return pathStructuredFieldPositions;
    }

    public void setRepeats(boolean repeats)
    {
        this.repeats = repeats;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((pathField == null) ? 0 : pathField.hashCode());
        result = prime * result + ((pathStructuredFieldPositions == null) ? 0 : pathStructuredFieldPositions.hashCode());
        result = prime * result + (repeats ? 1231 : 1237);
        result = prime * result + unitSize;
        return result;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        if (getClass() != obj.getClass())
            return false;
        SolrPathQuery other = (SolrPathQuery) obj;
        if (pathField == null)
        {
            if (other.pathField != null)
                return false;
        }
        else if (!pathField.equals(other.pathField))
            return false;
        if (pathStructuredFieldPositions == null)
        {
            if (other.pathStructuredFieldPositions != null)
                return false;
        }
        else if (!pathStructuredFieldPositions.equals(other.pathStructuredFieldPositions))
            return false;
        if (repeats != other.repeats)
            return false;
        if (unitSize != other.unitSize)
            return false;
        return true;
    }
    
    


}