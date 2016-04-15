/*
 * #%L
 * Alfresco Solr Client
 * %%
 * Copyright (C) 2005 - 2016 Alfresco Software Limited
 * %%
 * This file is part of the Alfresco software. 
 * If the software was purchased under a paid Alfresco license, the terms of 
 * the paid license agreement will prevail.  Otherwise, the software is 
 * provided under the following open source license terms:
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
 * #L%
 */
package org.alfresco.solr.tracker;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.alfresco.solr.InformationServerCollectionProvider;
import org.alfresco.solr.adapters.ISimpleOrderedMap;
import org.alfresco.util.Pair;

/**
 * @author Andy
 */
public class TrackerStats
{
    public static final int TIME_SCALE = 1000000;

    ConcurrentHashMap<String, IncrementalStats> modelTimes = new ConcurrentHashMap<String, IncrementalStats>();

    ConcurrentHashMap<String, IncrementalStats> aclTimes = new ConcurrentHashMap<String, IncrementalStats>();

    ConcurrentHashMap<String, IncrementalStats> changeSetAcls = new ConcurrentHashMap<String, IncrementalStats>();

    ConcurrentHashMap<String, IncrementalStats> txDocs = new ConcurrentHashMap<String, IncrementalStats>();

    ConcurrentHashMap<String, IncrementalStats> docTransformationTimes = new ConcurrentHashMap<String, IncrementalStats>();

    ConcurrentHashMap<String, IncrementalStats> nodeTimes = new ConcurrentHashMap<String, IncrementalStats>();
    
    ConcurrentHashMap<String, IncrementalStats> elapsedNodeTimes = new ConcurrentHashMap<String, IncrementalStats>();
    
    ConcurrentHashMap<String, IncrementalStats> elapsedAclTimes = new ConcurrentHashMap<String, IncrementalStats>();
    
    ConcurrentHashMap<String, IncrementalStats> elapsedContentTimes = new ConcurrentHashMap<String, IncrementalStats>();

    private InformationServerCollectionProvider infoSrv;
    
    public TrackerStats(InformationServerCollectionProvider server)
    {
        infoSrv = server;
    }
    
    /**
     * @return the modelTimes
     */
    public SimpleStats getModelTimes()
    {
        return aggregateResults(modelTimes);
    }

    /**
     * @param all ConcurrentHashMap<String, IncrementalStats>
     * @return SimpleStats
     */
    private SimpleStats aggregateResults(ConcurrentHashMap<String, IncrementalStats> all)
    {
        SimpleStats answer = null;

        for (String key : all.keySet())
        {
            IncrementalStats next = all.get(key);
            IncrementalStats stats = next.copy();
            if (answer == null)
            {
                answer = new SimpleStats(stats.scale, this.infoSrv);
                answer .start  = stats.start;
                answer.moments[0] = stats.moments[0];
                answer.moments[1] = stats.moments[1];
                answer.moments[2] = stats.moments[2];
                answer.max = stats.max;
                answer.min = stats.min;
                answer.copies.put(key, stats);
            }
            else
            {
                SimpleStats newAnswer = new SimpleStats(answer.scale, this.infoSrv);

                newAnswer.moments[0] = answer.moments[0] + stats.moments[0];

                newAnswer.moments[1] = answer.moments[1] * answer.moments[0] + stats.moments[1] * stats.moments[0];
                newAnswer.moments[1] /= answer.moments[0] + stats.moments[0];

                newAnswer.moments[2] = answer.moments[2] * answer.moments[0];
                newAnswer.moments[2] += (answer.moments[1] - newAnswer.moments[1]) * (answer.moments[1] - newAnswer.moments[1]) * answer.moments[0];
                newAnswer.moments[2] += stats.moments[2] * stats.moments[0];
                newAnswer.moments[2] += (stats.moments[1] - newAnswer.moments[1]) * (stats.moments[1] - newAnswer.moments[1]) * stats.moments[0];
                newAnswer.moments[2] /= answer.moments[0] + stats.moments[0];

                newAnswer.min = (stats.min < answer.min) ? stats.min : answer.min;
                newAnswer.max = (stats.max > answer.max) ? stats.max : answer.max;
                
                newAnswer.start = (stats.start.compareTo(answer.start) < 1) ? stats.start : answer.start;

                newAnswer.copies.putAll(answer.copies);
                newAnswer.copies.put(key, stats);

                answer = newAnswer;
            }

        }

        if (answer == null)
        {
            answer = new SimpleStats(1, this.infoSrv);
        }

        return answer;
    }

    /**
     * @return the aclTxTimes
     */
    public SimpleStats getAclTimes()
    {
        return aggregateResults(aclTimes);
    }

    public SimpleStats getChangeSetAcls()
    {
        return aggregateResults(changeSetAcls);
    }

    public SimpleStats getNodeTimes()
    {
        return aggregateResults(nodeTimes);
    }

    /**
     * @return the txDocs
     */
    public SimpleStats getTxDocs()
    {
        return aggregateResults(txDocs);
    }

    /**
     * @return the docTransformationTimes
     */
    public SimpleStats getDocTransformationTimes()
    {
        return aggregateResults(docTransformationTimes);
    }

    public double getMeanModelSyncTime()
    {
        return aggregateResults(modelTimes).getMean();
    }

    public double getMeanNodeIndexTime()
    {
        return aggregateResults(nodeTimes).getMean();
    }
    
    public double getMeanNodeElapsedIndexTime()
    {
        return aggregateResults(elapsedNodeTimes).getMean();
    }
    
    public double getMeanAclElapsedIndexTime()
    {
        return aggregateResults(elapsedAclTimes).getMean();
    }
    
    public double getMeanContentElapsedIndexTime()
    {
        return aggregateResults(elapsedContentTimes).getMean();
    }

    public double getNodeIndexingThreadCount()
    {
        return nodeTimes.size();
    }

    public double getMeanAclIndexTime()
    {
        return aggregateResults(nodeTimes).getMean();
    }

    public double getMeanDocsPerTx()
    {
        return aggregateResults(txDocs).getMean();
    }

    public double getMeanAclsPerChangeSet()
    {
        return aggregateResults(changeSetAcls).getMean();
    }

    public static class SimpleStats
    {
        HashMap<String, IncrementalStats> copies = new HashMap<String, IncrementalStats>();
        private InformationServerCollectionProvider server;
        int scale;

        SimpleStats(int scale, InformationServerCollectionProvider server)
        {
            this.scale = scale;
            this.server = server;
        }

        double[] moments = new double[3];

        double min = 0D;

        double max = 0D;

        Date start = null;
        
        synchronized long getN()
        {
            return (long) moments[0];
        }

        synchronized double getMin()
        {
            return min;
        }

        synchronized double getMax()
        {
            return max;
        }

        synchronized double getMean()
        {
            return moments[1];
        }

        synchronized double getVarience()
        {
            if (moments[0] > 1)
            {
                return moments[2] * moments[0] / (moments[0] - 1);
            }
            else
            {
                return Double.NaN;
            }
        }

        synchronized double getStandardDeviation()
        {
            return Math.sqrt(getVarience());
        }

        public synchronized ISimpleOrderedMap<Object> getNamedList(boolean incdludeDetail, boolean includeHist, boolean includeValues)
        {
            ISimpleOrderedMap<Object> map = this.server.getSimpleOrderedMapInstance();
            map.add("Start", start);
            map.add("N", getN());
            map.add("Min", getMin());
            map.add("Max", getMax());
            map.add("Mean", getMean());
            map.add("Varience", getVarience());
            map.add("StdDev", getStandardDeviation());
            if (incdludeDetail)
            {
                for (String key : copies.keySet())
                {
                    IncrementalStats value = copies.get(key);
                    map.add(key, value.getNamedList(includeHist, includeValues));
                }
            }

            return map;
        }

        /*
         * (non-Javadoc)
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString()
        {
            return "SimpleStats ["
                    + "\n             getN()=" + getN() + ",\n             getMin()=" + getMin() + ",\n             getMax()=" + getMax() + ",\n             getMean()="
                    + getMean() + ",\n             getVarience()=" + getVarience() + ",\n             getStandardDeviation()=" + getStandardDeviation() + ",\n             copies="
                    + copies + ",\n]";
        }

    }

    public static class Bucket
    {
        IncrementalStats incrementalStats;

        public double leftBoundary;

        public double rightBoundary;

        public double countLeft;

        public double countRight;

        Bucket(IncrementalStats incrementalStats, double leftBoundary, double rightBoundary)
        {
            this(incrementalStats, leftBoundary, rightBoundary, 0D, 0D);
        }

        Bucket(IncrementalStats incrementalStats, double leftBoundary, double rightBoundary, double countLeft, double countRight)
        {
            this.incrementalStats = incrementalStats;
            this.leftBoundary = leftBoundary;
            this.rightBoundary = rightBoundary;
            this.countLeft = countLeft;
            this.countRight = countRight;
        }

        public void add(double x)
        {
            if ((x - leftBoundary) < (rightBoundary - x))
            {
                countLeft++;
            }
            else
            {
                countRight++;
            }
        }

        public double mergeError(Bucket o)
        {
            double f_m = (this.countLeft + this.countRight + o.countLeft + o.countRight) / 4.0d;
            double t1 = this.countLeft - f_m;
            double t2 = this.countRight - f_m;
            double o1 = o.countLeft - f_m;
            double o2 = o.countRight - f_m;

            return (t1 * t1) + (t2 * t2) + (o1 * o1) + (o2 * o2);
        }

        public double error()
        {
            double f_m = (this.countLeft + this.countRight) / 2.0d;
            double t1 = this.countLeft - f_m;
            double t2 = this.countRight - f_m;
            return (t1 * t1) + (t2 * t2);

        }

        /*
         * (non-Javadoc)
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString()
        {
            double mark = (leftBoundary + rightBoundary) / 2.0D;
            double width = rightBoundary - leftBoundary;
            return "Bucket [("
                    + leftBoundary + " TO " + mark + " = " + countLeft / incrementalStats.getN() / width + ") : (" + mark + " TO " + rightBoundary + " = " + countRight
                    / incrementalStats.getN() / width + "]";
        }

    }

    public static class IncrementalStats
    {
        Date start = new Date();
        
        int scale;

        int buckets;

        double[] moments = new double[5];

        double min = 0D;

        double max = 0D;

        List<Double> values;

        List<Bucket> hist;
        
        InformationServerCollectionProvider server;

        public IncrementalStats(int scale, int buckets, InformationServerCollectionProvider infoSrv)
        {
            this.scale = scale;
            this.buckets = buckets;
            values = new ArrayList<Double>(buckets);
            hist = new ArrayList<Bucket>(buckets + 1);
            this.server = infoSrv;
        }

        /**
         * @return ISimpleOrderedMap
         */
        public ISimpleOrderedMap<Object> getNamedList(boolean includeHist, boolean includeValues)
        {
            ISimpleOrderedMap<Object> map = this.server.getSimpleOrderedMapInstance();
            map.add("Start", start);
            map.add("N", getN());
            map.add("Min", getMin());
            map.add("Max", getMax());
            map.add("Mean", getMean());
            map.add("Varience", getVarience());
            map.add("StdDev", getStandardDeviation());
            map.add("Skew", getSkew());
            map.add("Kurtosis", getKurtosis());

            if (includeHist)
            {
                int i = 0;
                ISimpleOrderedMap<Object> buckets = this.server.getSimpleOrderedMapInstance();
                for (Bucket b : hist)
                {
                    double mark = (b.leftBoundary + b.rightBoundary) / 2.0D;
                    double width = b.rightBoundary - b.leftBoundary;

                    // SimpleOrderedMap<Object> bucket = new SimpleOrderedMap<Object>();
                    // bucket.add("Lower", b.leftBoundary);
                    // bucket.add("Upper", mark);
                    // bucket.add("ProbabilityDensity", b.countLeft/b.incrementalStats.getN()/width);
                    buckets.add("" + i++, (b.leftBoundary + mark) / 2.0D + "," + b.countLeft / b.incrementalStats.getN() / width);

                    // bucket = new SimpleOrderedMap<Object>();
                    // bucket.add("Lower", mark);
                    // bucket.add("Upper", b.rightBoundary);
                    // bucket.add("ProbabilityDensity", b.countRight/b.incrementalStats.getN()/width);
                    buckets.add("" + i++, (mark + b.rightBoundary) / 2.0D + "," + b.countRight / b.incrementalStats.getN() / width);
                }
                map.add("Buckets", buckets);
            }

            if (includeValues)
            {
                int i = 0;
                ISimpleOrderedMap<Object> valuesMap = this.server.getSimpleOrderedMapInstance();
                for (Double value : values)
                {
                    valuesMap.add("" + i++, value);
                }
                map.add("Values", valuesMap);
            }

            return map;
        }

        public synchronized void reset()
        {
            moments = new double[5];

            min = 0D;

            max = 0D;

            values = new ArrayList<Double>(buckets);

            hist = new ArrayList<Bucket>(buckets + 1);
            
            start = new Date();
        }

        public synchronized void add(double xUnscaled)
        {
            double x = xUnscaled / scale;
            if ((moments[0] == 0) || (x > max))
            {
                max = x;
            }
            if ((moments[0] == 0L) || (x < min))
            {
                min = x;
            }
            double n = moments[0];
            double nPlus1 = n + 1;
            double n2 = n * n;
            double d = (moments[1] - x) / nPlus1;
            double d2 = d * d;
            double d3 = d2 * d;
            double n_nPlus1 = n / nPlus1;

            moments[4] += 4 * d * moments[3] + 6 * d2 * moments[2] + (1 + n * n2) * d2 * d2;
            moments[4] *= n_nPlus1;

            moments[3] += 3 * d * moments[2] + (1 - n2) * d3;
            moments[3] *= n_nPlus1;

            moments[2] += (1 + n) * d2;
            moments[2] *= n_nPlus1;

            moments[1] -= d;

            moments[0] = nPlus1;

            if (buckets > 1)
            {
                if (moments[0] < buckets)
                {
                    values.add(Double.valueOf(x));
                }
                else if (moments[0] == buckets)
                {
                    values.add(Double.valueOf(x));
                    buildInitialBuckets();
                }
                else
                {
                    values.set((int) moments[0] % buckets, x);
                    if (x < hist.get(0).leftBoundary)
                    {
                        double delta = (hist.get(0).leftBoundary - x) / 3.0;
                        Bucket b = new Bucket(this, x - delta, hist.get(0).leftBoundary);
                        hist.add(0, b);
                        b.add(x);
                        Pair<Integer, Double> bestToMerge = findBestToMerge();
                        if (hist.size() > buckets)
                        {
                            merge(bestToMerge.getFirst());
                        }
                    }
                    else if (x >= hist.get(hist.size() - 1).rightBoundary)
                    {
                        double delta = (x - hist.get(hist.size() - 1).rightBoundary) / 3.0;
                        Bucket b = new Bucket(this, hist.get(hist.size() - 1).rightBoundary, x + delta);
                        hist.add(b);
                        b.add(x);
                        Pair<Integer, Double> bestToMerge = findBestToMerge();
                        if (hist.size() > buckets)
                        {
                            merge(bestToMerge.getFirst());
                        }
                    }
                    else
                    {
                        // find existing
                        for (Bucket b : hist)
                        {
                            if ((b.leftBoundary <= x) && (x < b.rightBoundary))
                            {
                                b.add(x);
                                break;
                            }

                        }
                        Pair<Integer, Double> bestToMerge = findBestToMerge();
                        Pair<Integer, Double> bestToSplit = findBestToSplit();
                       
                        if (bestToMerge.getSecond() - bestToSplit.getSecond() < 0)
                        {
                            if(hist.size() < buckets)
                            {
                                split(bestToSplit.getFirst());
                            }
                            else
                            {
                                merge(bestToMerge.getFirst());
                                split(bestToMerge.getFirst() < bestToSplit.getFirst() ? bestToSplit.getFirst() - 1 : bestToSplit.getFirst());
                            }
                        }
                    }
                }
            }
        }

        /**
         * 
         */
        private void buildInitialBuckets()
        {
            // generate initial bucket list
            Collections.sort(values);
            FOUND: for (int i = 0; i < values.size(); i++)
            {
                for (Bucket b : hist)
                {
                    if ((b.leftBoundary <= values.get(i)) && (values.get(i) < b.rightBoundary))
                    {
                        b.add(values.get(i));
                        continue FOUND;
                    }

                }
                if (i < values.size() - 1)
                {
                    double start = values.get(i);
                    double end = start + 1.0D;

                    END: for (int j = i + 1; j < values.size(); j++)
                    {
                        if (values.get(j) > start)
                        {
                            end = values.get(j);
                            break END;
                        }
                    }

                    Bucket b = new Bucket(this, start, end);
                    b.add(values.get(i));
                    hist.add(b);
                }
                else
                {
                    double first = values.get(0);
                    double last = values.get(values.size() - 1);
                    double width = 1.0D;
                    if (values.size() > 1)
                    {
                        width = (last - first) / (values.size() - 1);
                    }
                    Bucket b = new Bucket(this, last, last + width);
                    b.add(values.get(i));
                    hist.add(b);
                }
            }
        }

        void merge(int position)
        {
            Bucket lower = hist.get(position);
            Bucket upper = hist.get(position + 1);
            Bucket merged = new Bucket(this, lower.leftBoundary, upper.rightBoundary, lower.countLeft + lower.countRight, upper.countLeft + upper.countRight);
            hist.remove(position);
            hist.set(position, merged);

        }

        void split(int position)
        {
            Bucket toSplit = hist.get(position);
            double mark = (toSplit.leftBoundary + toSplit.rightBoundary) / 2.0D;
            Bucket lower = new Bucket(this, toSplit.leftBoundary, mark, toSplit.countLeft / 2.0D, toSplit.countLeft / 2.0D);
            Bucket upper = new Bucket(this, mark, toSplit.rightBoundary, toSplit.countRight / 2.0D, toSplit.countRight / 2.0D);
            hist.set(position, upper);
            hist.add(position, lower);
        }

        Pair<Integer, Double> findBestToMerge()
        {
            double minMergeError = Double.MAX_VALUE;
            int bucket = 0;
            for (int i = 0; i < hist.size() - 1; i++)
            {
                double mergeError = hist.get(i).mergeError(hist.get(i + 1));
                if (mergeError < minMergeError)
                {
                    minMergeError = mergeError;
                    bucket = i;
                }
            }
            return new Pair<Integer, Double>(bucket, minMergeError);
        }

        Pair<Integer, Double> findBestToSplit()
        {
            double maxError = Double.MIN_VALUE;
            int bucket = 0;
            for (int i = 0; i < hist.size(); i++)
            {
                double error = hist.get(i).error();
                if (error > maxError)
                {
                    maxError = error;
                    bucket = i;
                }
            }
            return new Pair<Integer, Double>(bucket, maxError);
        }

        public synchronized long getN()
        {
            return (long) moments[0];
        }

        public synchronized double getMin()
        {
            return min;
        }

        public synchronized double getMax()
        {
            return max;
        }

        public synchronized double getMean()
        {
            return moments[1];
        }

        public synchronized double getVarience()
        {
            if (moments[0] > 1)
            {
                return moments[2] * moments[0] / (moments[0] - 1);
            }
            else
            {
                return Double.NaN;
            }
        }

        public synchronized double getStandardDeviation()
        {
            return Math.sqrt(getVarience());
        }

        public synchronized double getSkew()
        {
            if (moments[0] > 2)
            {
                double v = getVarience();
                return moments[3] * moments[0] * moments[0] / (Math.sqrt(v) * v * (moments[0] - 1) * (moments[0] - 2));
            }
            else
            {
                return Double.NaN;
            }
        }

        public synchronized double getKurtosis()
        {
            if (moments[0] > 3)
            {
                double div = (moments[0] - 2) * (moments[0] - 3);
                double nMinus1 = moments[0] - 1;
                double v = getVarience();
                double z = ((moments[4] * moments[0] * moments[0] * (moments[0] + 1)) / (v * v * nMinus1));
                z -= 3 * nMinus1 * nMinus1;
                z /= div;
                return z;
            }
            else
            {
                return Double.NaN;
            }
        }
        
        public synchronized List<Bucket> getHistogram()
        {
            if(hist.size() == 0)
            {
                buildInitialBuckets();
            }
            return hist;
        }
        
     

        synchronized IncrementalStats copy()
        {
            IncrementalStats copy = new IncrementalStats(this.scale, this.buckets, this.server);
            copy.start = this.start;
            copy.max = this.max;
            copy.min = this.min;
            copy.moments[0] = this.moments[0];
            copy.moments[1] = this.moments[1];
            copy.moments[2] = this.moments[2];
            copy.moments[3] = this.moments[3];
            copy.moments[4] = this.moments[4];
            for (Double x : this.values)
            {
                copy.values.add(x);
            }
            for (Bucket b : this.hist)
            {
                copy.hist.add(new Bucket(copy, b.leftBoundary, b.rightBoundary, b.countLeft, b.countRight));
            }
            return copy;
        }

        /*
         * (non-Javadoc)
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString()
        {
            return "IncrementalStats [getN()="
                    + getN() + ", getMin()=" + getMin() + ", getMax()=" + getMax() + ", getMean()=" + getMean() + ", getVarience()=" + getVarience() + ", getStandardDeviation()="
                    + getStandardDeviation() + ", getSkew()=" + getSkew() + ", getKurtosis()=" + getKurtosis() + ", values=" + values + ", hist=" + hist + "]";
        }

    }

    /**
     * @param time long
     */
    public void addModelTime(long time)
    {
        IncrementalStats stats = modelTimes.get(Thread.currentThread().getName());
        if (stats == null)
        {
            stats = new IncrementalStats(TIME_SCALE, 50, this.infoSrv);
            modelTimes.put(Thread.currentThread().getName(), stats);
        }
        stats.add(time);
    }

    /**
     * @param time long
     */
    public void addAclTime(long time)
    {
        IncrementalStats stats = aclTimes.get(Thread.currentThread().getName());
        if (stats == null)
        {
            stats = new IncrementalStats(TIME_SCALE, 50, this.infoSrv);
            aclTimes.put(Thread.currentThread().getName(), stats);
        }
        stats.add(time);
    }

    /**
     * @param time long
     */
    public void addNodeTime(long time)
    {
        IncrementalStats stats = nodeTimes.get(Thread.currentThread().getName());
        if (stats == null)
        {
            stats = new IncrementalStats(TIME_SCALE, 50, this.infoSrv);
            nodeTimes.put(Thread.currentThread().getName(), stats);
        }
        stats.add(time);
    }

    /**
     * @param docCount int
     * @param time long
     */
    public void addElapsedNodeTime(int docCount, long time)
    {
        if(docCount < 1)
        {
            return;
        }
        IncrementalStats stats = elapsedNodeTimes.get(Thread.currentThread().getName());
        if (stats == null)
        {
            stats = new IncrementalStats(TIME_SCALE, 50, this.infoSrv);
            elapsedNodeTimes.put(Thread.currentThread().getName(), stats);
        }
        long meanTime = time / docCount;
        for(int i = 0; i < docCount; i++)
        {
            stats.add(meanTime);
        }
        
    }
    
    /**
     * @param docCount int
     * @param time long
     */
    public void addElapsedAclTime(int docCount, long time)
    {
        if(docCount < 1)
        {
            return;
        }
        IncrementalStats stats = elapsedAclTimes.get(Thread.currentThread().getName());
        if (stats == null)
        {
            stats = new IncrementalStats(TIME_SCALE, 50, this.infoSrv);
            elapsedAclTimes.put(Thread.currentThread().getName(), stats);
        }
        long meanTime = time / docCount;
        for(int i = 0; i < docCount; i++)
        {
            stats.add(meanTime);
        }
        
    }
    
    /**
     * @param docCount int
     * @param time long
     */
    public void addElapsedContentTime(int docCount, long time)
    {
        if(docCount < 1)
        {
            return;
        }
        IncrementalStats stats = elapsedContentTimes.get(Thread.currentThread().getName());
        if (stats == null)
        {
            stats = new IncrementalStats(TIME_SCALE, 50, this.infoSrv);
            elapsedContentTimes.put(Thread.currentThread().getName(), stats);
        }
        long meanTime = time / docCount;
        for(int i = 0; i < docCount; i++)
        {
            stats.add(meanTime);
        }
        
    }
    
    /**
     * @param size int
     */
    public void addTxDocs(int size)
    {
        IncrementalStats stats = txDocs.get(Thread.currentThread().getName());
        if (stats == null)
        {
            stats = new IncrementalStats(1, 50, this.infoSrv);
            txDocs.put(Thread.currentThread().getName(), stats);
        }
        stats.add(size);
    }

    /**
     * @param size int
     */
    public void addChangeSetAcls(int size)
    {
        IncrementalStats stats = changeSetAcls.get(Thread.currentThread().getName());
        if (stats == null)
        {
            stats = new IncrementalStats(1, 50, this.infoSrv);
            changeSetAcls.put(Thread.currentThread().getName(), stats);
        }
        stats.add(size);
    }

    /**
     * @param time long
     */
    public void addDocTransformationTime(long time)
    {
        IncrementalStats stats = docTransformationTimes.get(Thread.currentThread().getName());
        if (stats == null)
        {
            stats = new IncrementalStats(TIME_SCALE, 50, this.infoSrv);
            docTransformationTimes.put(Thread.currentThread().getName(), stats);
        }
        stats.add(time);
    }

    /*
     * (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString()
    {
        return "TrackerStats [modelTimes="
                + modelTimes + ", aclTimes=" + aclTimes + ", changeSetAcls=" + changeSetAcls + ", txDocs=" + txDocs + ", docTransformationTimes=" + docTransformationTimes
                + ", nodeTimes=" + nodeTimes + "]";
    }

    /**
     * 
     */
    public void reset()
    {
        modelTimes.clear();
        aclTimes.clear();
        changeSetAcls.clear();
        txDocs.clear();
        docTransformationTimes.clear();
        nodeTimes.clear();
    }

 
}
