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

import com.google.common.collect.ImmutableMap;
import org.alfresco.solr.AlfrescoSolrDataModel;
import org.apache.calcite.rel.type.*;
import org.apache.calcite.schema.Table;
import org.apache.calcite.schema.impl.AbstractSchema;
import org.apache.calcite.sql.type.SqlTypeFactoryImpl;
import org.apache.lucene.index.FieldInfo;
import org.apache.lucene.index.LeafReader;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.apache.solr.client.solrj.request.LukeRequest;
import org.apache.solr.client.solrj.response.LukeResponse;
import org.apache.solr.common.util.SimpleOrderedMap;
import org.apache.solr.core.SolrCore;
import org.apache.solr.schema.FieldType;
import org.apache.solr.schema.IndexSchema;
import org.apache.solr.schema.SchemaField;
import org.apache.solr.search.SolrIndexSearcher;
import org.apache.solr.util.RefCounted;

import java.io.IOException;
import java.util.*;

class SolrSchema extends AbstractSchema {
  final Properties properties;
  final SolrCore core;

  SolrSchema(SolrCore core, Properties properties) {
    super();
    this.core = core;
    this.properties = properties;
  }

  @Override
  protected Map<String, Table> getTableMap() {
    Map<String, Table> map = new HashMap();
    map.put("alfresco", new SolrTable(this, "alfresco"));
    return map;
  }


  RelProtoDataType getRelDataType(String collection) {

    final RelDataTypeFactory typeFactory = new SqlTypeFactoryImpl(RelDataTypeSystem.DEFAULT);
    final RelDataTypeFactory.FieldInfoBuilder fieldInfo = typeFactory.builder();

    Map<String, String> fields = getIndexedFieldsInfo();

    Set<Map.Entry<String, String>> set = fields.entrySet();

      for(Map.Entry<String, String> entry : set) {
      String ltype = entry.getValue();

      RelDataType type;
      switch (ltype) {
        case "solr.StrField":
        case "solr.TextField":
        case "org.alfresco.solr.AlfrescoFieldType":
          type = typeFactory.createJavaType(String.class);
          break;
        case "solr.TrieLongField":
          type = typeFactory.createJavaType(Long.class);
          break;
        case "solr.TrieDoubleField":
          type = typeFactory.createJavaType(Double.class);
          break;
        case "solr.TrieFloatField":
          type = typeFactory.createJavaType(Double.class);
          break;
        case "solr.TrieIntField":
          type = typeFactory.createJavaType(Long.class);
          break;
        default:
          type = typeFactory.createJavaType(String.class);
      }

      fieldInfo.add(entry.getKey(), type).nullable(true);
    }
    fieldInfo.add("_query_",typeFactory.createJavaType(String.class));
    fieldInfo.add("score",typeFactory.createJavaType(Double.class));

    return RelDataTypeImpl.proto(fieldInfo.build());
  }

  private Map<String, String> getIndexedFieldsInfo() throws RuntimeException {

    RefCounted<SolrIndexSearcher> refCounted = core.getSearcher();
    SolrIndexSearcher searcher = null;
    try {
      searcher = refCounted.get();
      LeafReader reader = searcher.getSlowAtomicReader();
      IndexSchema schema = searcher.getSchema();

      Set<String> fieldNames = new TreeSet<>();
      for (FieldInfo fieldInfo : reader.getFieldInfos()) {
        fieldNames.add(fieldInfo.name);
      }
      Map fieldMap = new HashMap();
      for (String fieldName : fieldNames) {
        SchemaField sfield = schema.getFieldOrNull(fieldName);
        FieldType ftype = (sfield == null) ? null : sfield.getType();

        // Add the field
        fieldMap.put(AlfrescoSolrDataModel.getInstance().getAlfrescoPropertyFromSchemaField(fieldName), ftype.getClassArg());
      }

      return fieldMap;
    } finally {
      refCounted.decref();
    }
  }
}
