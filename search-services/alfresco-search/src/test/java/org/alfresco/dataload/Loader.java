/*
 * Copyright (C) 2005-2017 Alfresco Software Limited.
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
package org.alfresco.dataload;

import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.request.UpdateRequest;
import org.apache.solr.common.SolrInputDocument;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Random;

public class Loader {

    public static void main(String args[]) throws Exception {
        int num = Integer.parseInt(args[0]);
        int start = Integer.parseInt(args[1]);
        //String url = "http://localhost:8985/solr/joel";
        String url = "http://localhost:8983/solr/collection1";

        HttpSolrClient client = new HttpSolrClient(url);
        UpdateRequest request = new UpdateRequest();
        int i = start;
        LocalDateTime localDate = LocalDateTime.now();

        Random rand = new Random();
        for(i=start; i<num+start; i++) {
            String s = rand.nextInt(1000)+"helloworld123";
            SolrInputDocument doc = new SolrInputDocument();
            doc.addField("id", i);
            doc.addField("test_s", s);
            doc.addField("test_t", "hello world we love you");
            int year = rand.nextInt(50);
            int month = rand.nextInt(12);
            int day = rand.nextInt(30);
            float f = rand.nextFloat();

            doc.addField("year_i", Integer.toString(year));
            doc.addField("month_i", Integer.toString(month));
            doc.addField("day_i", Integer.toString(day));
            doc.addField("price_f", Float.toString(f));

            LocalDateTime randomDate = localDate.plusDays(rand.nextInt(1000));
            doc.addField("date_dt", DateTimeFormatter.ISO_INSTANT.format(randomDate.toInstant(ZoneOffset.UTC)));
            doc.addField("epoch_l", randomDate.atZone(ZoneOffset.UTC).toInstant().toEpochMilli());

            request.add(doc);
            if(i % 5000 == 0) {
                request.process(client);
                client.commit();
                request = new UpdateRequest();
            }


            for(int l=0; l<5; l++) {
                String ps = "product"+rand.nextInt(35);
                doc.addField("prod_ss",ps);
            }

        }

        if((i % 5000) != 0) {
            request.process(client);
            client.commit();
        }

        client.close();

    }


}
