package org.alfresco.solr.basics;

import org.apache.solr.client.solrj.SolrResponse;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.util.NamedList;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SolrResponsesComparator
{
    private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    
    protected int flags;
    
    protected Map<String, Integer> handle = new HashMap<>();

    public static int ORDERED = 1;
    public static int SKIP = 2;
    public static int SKIPVAL = 4;
    public static int UNORDERED = 8;

    /**
     * When this flag is set, Double values will be allowed a difference ratio
     * of 1E-8 between the non-distributed and the distributed returned values
     */
    public static int FUZZY = 16;
    private static final double DOUBLE_RATIO_LIMIT = 1E-8;
    
    /**
     * Puts default values for handle
     */
    public void putHandleDefaults() {
        handle.put("explain", SKIPVAL);
        handle.put("timestamp", SKIPVAL);
        handle.put("score", SKIPVAL);
        handle.put("wt", SKIP);
        handle.put("distrib", SKIP);
        handle.put("shards.qt", SKIP);
        handle.put("shards", SKIP);
        handle.put("q", SKIP);
        handle.put("maxScore", SKIPVAL);
        handle.put("_version_", SKIP);
        handle.put("_original_parameters_", SKIP);
        handle.put("spellcheck-extras", SKIP); // No longer used can be removed in Solr 6.

    }
    

    public void compareSolrResponses(SolrResponse a, SolrResponse b)
    {
        // SOLR-3345: Checking QTime value can be skipped as there is no
        // guarantee that the numbers will match.
        handle.put("QTime", SKIPVAL);
        String cmp = compare(a.getResponse(), b.getResponse(), flags, handle);
        if (cmp != null)
        {
            log.error("Mismatched responses:\n" + a + "\n" + b);
            Assert.fail(cmp);
        }
    }
    
    /**
     * Validates that solr instance is running and that query was processed.
     * @param response
     */
    public void validateResponse(QueryResponse response)
    {
        switch (response.getStatus())
        {
            case 500:
                throw new RuntimeException("Solr instance internal server error 500");

            default:
                break;
        }
    }
    
    public void compareResponses(QueryResponse a, QueryResponse b)
    {
        if (System.getProperty("remove.version.field") != null)
        {
            // we don't care if one has a version and the other doesnt -
            // control vs distrib
            // TODO: this should prob be done by adding an ignore on _version_
            // rather than mutating the responses?
            if (a.getResults() != null)
            {
                for (SolrDocument doc : a.getResults())
                {
                    doc.removeFields("_version_");
                }
            }
            if (b.getResults() != null)
            {
                for (SolrDocument doc : b.getResults())
                {
                    doc.removeFields("_version_");
                }
            }
        }
        compareSolrResponses(a, b);
    }
    
    public static String compare(NamedList a, NamedList b, int flags, Map<String, Integer> handle)
    {
        // System.out.println("resp a:" + a);
        // System.out.println("resp b:" + b);
        boolean ordered = (flags & UNORDERED) == 0;

        if (!ordered)
        {
            Map mapA = new HashMap(a.size());
            for (int i = 0; i < a.size(); i++)
            {
                Object prev = mapA.put(a.getName(i), a.getVal(i));
            }

            Map mapB = new HashMap(b.size());
            for (int i = 0; i < b.size(); i++)
            {
                Object prev = mapB.put(b.getName(i), b.getVal(i));
            }

            return compare(mapA, mapB, flags, handle);
        }

        int posa = 0, posb = 0;
        int aSkipped = 0, bSkipped = 0;

        for (;;)
        {
            if (posa >= a.size() && posb >= b.size())
            {
                break;
            }

            String namea = null, nameb = null;
            Object vala = null, valb = null;

            int flagsa = 0, flagsb = 0;
            while (posa < a.size())
            {
                namea = a.getName(posa);
                vala = a.getVal(posa);
                posa++;
                flagsa = flags(handle, namea);
                if ((flagsa & SKIP) != 0)
                {
                    namea = null;
                    vala = null;
                    aSkipped++;
                    continue;
                }
                break;
            }

            while (posb < b.size())
            {
                nameb = b.getName(posb);
                valb = b.getVal(posb);
                posb++;
                flagsb = flags(handle, nameb);
                if ((flagsb & SKIP) != 0)
                {
                    nameb = null;
                    valb = null;
                    bSkipped++;
                    continue;
                }
                if (eq(namea, nameb))
                {
                    break;
                }
                return "." + namea + "!=" + nameb + " (unordered or missing)";
                // if unordered, continue until we find the right field.
            }

            // ok, namea and nameb should be equal here already.
            if ((flagsa & SKIPVAL) != 0)
                continue; // keys matching is enough

            String cmp = compare(vala, valb, flagsa, handle);
            if (cmp != null)
                return "." + namea + cmp;
        }

        if (a.size() - aSkipped != b.size() - bSkipped)
        {
            return ".size()==" + a.size() + "," + b.size() + " skipped=" + aSkipped + "," + bSkipped;
        }

        return null;
    }

    public static String compare1(Map a, Map b, int flags, Map<String, Integer> handle)
    {
        String cmp;

        for (Object keya : a.keySet())
        {
            Object vala = a.get(keya);
            int flagsa = flags(handle, keya);
            if ((flagsa & SKIP) != 0)
                continue;
            if (!b.containsKey(keya))
            {
                return "[" + keya + "]==null";
            }
            if ((flagsa & SKIPVAL) != 0)
                continue;
            Object valb = b.get(keya);
            cmp = compare(vala, valb, flagsa, handle);
            if (cmp != null)
                return "[" + keya + "]" + cmp;
        }
        return null;
    }

    public static String compare(Map a, Map b, int flags, Map<String, Integer> handle)
    {
        String cmp;
        cmp = compare1(a, b, flags, handle);
        if (cmp != null)
            return cmp;
        return compare1(b, a, flags, handle);
    }

    public static String compare(SolrDocument a, SolrDocument b, int flags, Map<String, Integer> handle)
    {
        return compare(a.getFieldValuesMap(), b.getFieldValuesMap(), flags, handle);
    }

    public static String compare(SolrDocumentList a, SolrDocumentList b, int flags, Map<String, Integer> handle)
    {
        boolean ordered = (flags & UNORDERED) == 0;

        String cmp;
        int f = flags(handle, "maxScore");
        if (f == 0)
        {
            cmp = compare(a.getMaxScore(), b.getMaxScore(), 0, handle);
            if (cmp != null)
                return ".maxScore" + cmp;
        } else if ((f & SKIP) == 0)
        { // so we skip val but otherwise both should be present
            assert (f & SKIPVAL) != 0;
            if (b.getMaxScore() != null)
            {
                if (a.getMaxScore() == null)
                {
                    return ".maxScore missing";
                }
            }
        }

        cmp = compare(a.getNumFound(), b.getNumFound(), 0, handle);
        if (cmp != null)
            return ".numFound" + cmp;

        cmp = compare(a.getStart(), b.getStart(), 0, handle);
        if (cmp != null)
            return ".start" + cmp;

        cmp = compare(a.size(), b.size(), 0, handle);
        if (cmp != null)
            return ".size()" + cmp;

        // only for completely ordered results (ties might be in a different
        // order)
        if (ordered)
        {
            for (int i = 0; i < a.size(); i++)
            {
                cmp = compare(a.get(i), b.get(i), 0, handle);
                if (cmp != null)
                    return "[" + i + "]" + cmp;
            }
            return null;
        }

        // unordered case
        for (int i = 0; i < a.size(); i++)
        {
            SolrDocument doc = a.get(i);
            Object key = doc.getFirstValue("id");
            SolrDocument docb = null;
            if (key == null)
            {
                // no id field to correlate... must compare ordered
                docb = b.get(i);
            } else
            {
                for (int j = 0; j < b.size(); j++)
                {
                    docb = b.get(j);
                    if (key.equals(docb.getFirstValue("id")))
                        break;
                }
            }
            // if (docb == null) return "[id="+key+"]";
            cmp = compare(doc, docb, 0, handle);
            if (cmp != null)
                return "[id=" + key + "]" + cmp;
        }
        return null;
    }

    public static String compare(Object[] a, Object[] b, int flags, Map<String, Integer> handle)
    {
        if (a.length != b.length)
        {
            return ".length:" + a.length + "!=" + b.length;
        }
        for (int i = 0; i < a.length; i++)
        {
            String cmp = compare(a[i], b[i], flags, handle);
            if (cmp != null)
                return "[" + i + "]" + cmp;
        }
        return null;
    }

    public static String compare(Object a, Object b, int flags, Map<String, Integer> handle)
    {
        if (a == b)
            return null;
        if (a == null || b == null)
            return ":" + a + "!=" + b;

        if (a instanceof NamedList && b instanceof NamedList)
        {
            return compare((NamedList) a, (NamedList) b, flags, handle);
        }

        if (a instanceof SolrDocumentList && b instanceof SolrDocumentList)
        {
            return compare((SolrDocumentList) a, (SolrDocumentList) b, flags, handle);
        }

        if (a instanceof SolrDocument && b instanceof SolrDocument)
        {
            return compare((SolrDocument) a, (SolrDocument) b, flags, handle);
        }

        if (a instanceof Map && b instanceof Map)
        {
            return compare((Map) a, (Map) b, flags, handle);
        }

        if (a instanceof Object[] && b instanceof Object[])
        {
            return compare((Object[]) a, (Object[]) b, flags, handle);
        }

        if (a instanceof byte[] && b instanceof byte[])
        {
            if (!Arrays.equals((byte[]) a, (byte[]) b))
            {
                return ":" + a + "!=" + b;
            }
            return null;
        }

        if (a instanceof List && b instanceof List)
        {
            return compare(((List) a).toArray(), ((List) b).toArray(), flags, handle);

        }

        if ((flags & FUZZY) != 0)
        {
            if ((a instanceof Double && b instanceof Double))
            {
                double aaa = ((Double) a).doubleValue();
                double bbb = ((Double) b).doubleValue();
                if (aaa == bbb || ((Double) a).isNaN() && ((Double) b).isNaN())
                {
                    return null;
                }
                if ((aaa == 0.0) || (bbb == 0.0))
                {
                    return ":" + a + "!=" + b;
                }

                double diff = Math.abs(aaa - bbb);
                // When stats computations are done on multiple shards, there
                // may
                // be small differences in the results. Allow a small difference
                // between the result of the computations.

                double ratio = Math.max(Math.abs(diff / aaa), Math.abs(diff / bbb));
                if (ratio > DOUBLE_RATIO_LIMIT)
                {
                    return ":" + a + "!=" + b;
                } else
                {
                    return null;// close enough.
                }
            }
        }

        if (!(a.equals(b)))
        {
            return ":" + a + "!=" + b;
        }

        return null;
    }

    public static boolean eq(String a, String b)
    {
        return a == b || (a != null && a.equals(b));
    }

    public static int flags(Map<String, Integer> handle, Object key)
    {
        if (handle == null)
            return 0;
        Integer f = handle.get(key);
        return f == null ? 0 : f;
    }
}
