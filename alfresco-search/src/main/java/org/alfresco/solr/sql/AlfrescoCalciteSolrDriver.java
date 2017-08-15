/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.alfresco.solr.sql;

import org.apache.calcite.jdbc.CalciteConnection;
import org.apache.calcite.jdbc.Driver;
import org.apache.calcite.schema.SchemaPlus;
import org.apache.solr.core.SolrCore;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;
import java.util.Map;
import java.util.HashMap;

/**
 * JDBC driver for Calcite Solr.
 *
 * <p>It accepts connect strings that start with "jdbc:calcitesolr:".</p>
 */
public class AlfrescoCalciteSolrDriver extends Driver {

  private final static Map<String, SolrCore> cores = new HashMap();

  public final static String CONNECT_STRING_PREFIX = "jdbc:alfrescosolr:";

  private AlfrescoCalciteSolrDriver() {
    super();
  }

  static {
    new AlfrescoCalciteSolrDriver().register();
  }

  @Override
  protected String getConnectStringPrefix() {
    return CONNECT_STRING_PREFIX;
  }

  public static synchronized void registerCore(SolrCore core) {
    cores.put(core.getName(), core);
  }

  public static synchronized SolrCore getCore(String coreName) {
    return cores.get(coreName);
  }

  @Override
  public Connection connect(String url, Properties info) throws SQLException {
    if(!this.acceptsURL(url)) {
      return null;
    }

    String localCore = info.getProperty("localCore");
    SolrCore core = getCore(localCore);

    Connection connection = super.connect(url, info);
    CalciteConnection calciteConnection = (CalciteConnection) connection;
    final SchemaPlus rootSchema = calciteConnection.getRootSchema();


    rootSchema.add("alfresco", new SolrSchema(core, info));

    // Set the default schema
    calciteConnection.setSchema("alfresco");

    return connection;
  }
}
