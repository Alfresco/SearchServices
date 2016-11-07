/*
 * Copyright (C) 2005-2016 Alfresco Software Limited.
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

import org.alfresco.service.namespace.QName;
/**
 * Trait to mix in constants in to test classes.
 * @author Michael Suzuki
 *
 */
public interface AlfrecsoSolrConstants
{
    final String TEST_NAMESPACE = "http://www.alfresco.org/test/solrtest";
    final String CMIS_TEST_NAMESPACE = "http://www.alfresco.org/test/cmis-query-test";
    final static QName createdDate = QName.createQName(TEST_NAMESPACE, "createdDate");
    final static QName createdTime = QName.createQName(TEST_NAMESPACE, "createdTime");
    final static QName orderDouble = QName.createQName(TEST_NAMESPACE, "orderDouble");
    final static QName orderFloat = QName.createQName(TEST_NAMESPACE, "orderFloat");
    final static QName orderLong = QName.createQName(TEST_NAMESPACE, "orderLong");
    final static QName orderInt = QName.createQName(TEST_NAMESPACE, "orderInt");
    final static QName orderText = QName.createQName(TEST_NAMESPACE, "orderText");
    final static QName orderLocalisedText = QName.createQName(TEST_NAMESPACE, "orderLocalisedText");
    final static QName orderMLText = QName.createQName(TEST_NAMESPACE, "orderMLText");
    final static QName orderLocalisedMLText = QName.createQName(TEST_NAMESPACE, "orderLocalisedMLText");
    final static QName testSuperType = QName.createQName(TEST_NAMESPACE, "testSuperType");
    final static QName testType = QName.createQName(TEST_NAMESPACE, "testType");
    final static QName testAspect = QName.createQName(TEST_NAMESPACE, "testAspect");
    final static QName extendedContent = QName.createQName(CMIS_TEST_NAMESPACE, "extendedContent");
    final static QName singleTextBoth = QName.createQName(CMIS_TEST_NAMESPACE, "singleTextBoth");
    final static QName singleTextUntokenised = QName.createQName(CMIS_TEST_NAMESPACE, "singleTextUntokenised");
    final static QName singleTextTokenised = QName.createQName(CMIS_TEST_NAMESPACE, "singleTextTokenised");
    final static QName multipleTextBoth = QName.createQName(CMIS_TEST_NAMESPACE, "multipleTextBoth");
    final static QName multipleTextUntokenised = QName.createQName(CMIS_TEST_NAMESPACE, "multipleTextUntokenised");
    final static QName multipleTextTokenised = QName.createQName(CMIS_TEST_NAMESPACE, "multipleTextTokenised");
    final static QName singleMLTextBoth = QName.createQName(CMIS_TEST_NAMESPACE, "singleMLTextBoth");
    final static QName singleMLTextUntokenised = QName.createQName(CMIS_TEST_NAMESPACE, "singleMLTextUntokenised");
    final static QName singleMLTextTokenised = QName.createQName(CMIS_TEST_NAMESPACE, "singleMLTextTokenised");
    final static QName multipleMLTextBoth = QName.createQName(CMIS_TEST_NAMESPACE, "multipleMLTextBoth");
    final static QName multipleMLTextUntokenised = QName.createQName(CMIS_TEST_NAMESPACE, "multipleMLTextUntokenised");
    final static QName multipleMLTextTokenised = QName.createQName(CMIS_TEST_NAMESPACE, "multipleMLTextTokenised");
    final static QName singleFloat = QName.createQName(CMIS_TEST_NAMESPACE, "singleFloat");
    final static QName multipleFloat = QName.createQName(CMIS_TEST_NAMESPACE, "multipleFloat");
    final static QName singleDouble = QName.createQName(CMIS_TEST_NAMESPACE, "singleDouble");
    final static QName multipleDouble = QName.createQName(CMIS_TEST_NAMESPACE, "multipleDouble");
    final static QName singleInteger = QName.createQName(CMIS_TEST_NAMESPACE, "singleInteger");
    final static QName multipleInteger = QName.createQName(CMIS_TEST_NAMESPACE, "multipleInteger");
    final static QName singleLong = QName.createQName(CMIS_TEST_NAMESPACE, "singleLong");
    final static QName multipleLong = QName.createQName(CMIS_TEST_NAMESPACE, "multipleLong");
    final static QName singleBoolean = QName.createQName(CMIS_TEST_NAMESPACE, "singleBoolean");
    final static QName multipleBoolean = QName.createQName(CMIS_TEST_NAMESPACE, "multipleBoolean");
    final static QName singleDate = QName.createQName(CMIS_TEST_NAMESPACE, "singleDate");
    final static QName multipleDate = QName.createQName(CMIS_TEST_NAMESPACE, "multipleDate");
    final static QName singleDatetime = QName.createQName(CMIS_TEST_NAMESPACE, "singleDatetime");
    final static QName multipleDatetime = QName.createQName(CMIS_TEST_NAMESPACE, "multipleDatetime");
}
