package org.alfresco.solr.basics;

import java.util.Date;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import static org.apache.lucene.util.LuceneTestCase.random;

public class RandomSupplier
{
    protected Random randomGenerator;
    
    public RandomSupplier()
    {
        this.randomGenerator = new Random(random().nextLong());
    }

    public Random getRandomGenerator()
    {
        return randomGenerator;
    }

    public RandVal rint = new RandVal()
    {
        @Override
        public Object val()
        {
            return randomGenerator.nextInt();
        }
    };

    public RandVal rlong = new RandVal()
    {
        @Override
        public Object val()
        {
            return randomGenerator.nextLong();
        }
    };

    public RandVal rfloat = new RandVal()
    {
        @Override
        public Object val()
        {
            return randomGenerator.nextFloat();
        }
    };

    public RandVal rdouble = new RandVal()
    {
        @Override
        public Object val()
        {
            return randomGenerator.nextDouble();
        }
    };

    public RandVal rdate = new RandDate();

    public RandVal[] randVals = new RandVal[]
        { rint, rfloat, rfloat, rdouble, rdouble, rlong, rlong, rdate, rdate };

    public RandVal[] getRandValues()
    {
        return randVals;
    }

    public Object[] getRandFields(String[] fields, RandVal[] randVals)
    {
        Object[] o = new Object[fields.length * 2];
        for (int i = 0; i < fields.length; i++)
        {
            o[i * 2] = fields[i];
            o[i * 2 + 1] = randVals[i].uval();
        }
        return o;
    }

    public static abstract class RandVal
    {
        public static Set uniqueValues = new HashSet();

        public abstract Object val();

        public Object uval()
        {
            for (;;)
            {
                Object v = val();
                if (uniqueValues.add(v))
                    return v;
            }
        }
    }

    public class RandDate extends RandVal
    {
        @Override
        public Object val()
        {
            long v = randomGenerator.nextLong();
            Date d = new Date(v);
            return d.toInstant().toString();
        }
    }
    
}
