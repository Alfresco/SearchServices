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
 *
 * @author Michael Suzuki
 */
public interface AlfrescoSolrConstants
{
    String TEST_NAMESPACE = "http://www.alfresco.org/test/solrtest";
    String CMIS_TEST_NAMESPACE = "http://www.alfresco.org/test/cmis-query-test";

    QName CREATED_DATE = QName.createQName(TEST_NAMESPACE, "createdDate");
    QName CREATED_TIME = QName.createQName(TEST_NAMESPACE, "createdTime");
    QName ORDER_DOUBLE = QName.createQName(TEST_NAMESPACE, "orderDouble");
    QName ORDER_FLOAT = QName.createQName(TEST_NAMESPACE, "orderFloat");
    QName ORDER_LONG = QName.createQName(TEST_NAMESPACE, "orderLong");
    QName ORDER_INT = QName.createQName(TEST_NAMESPACE, "orderInt");
    QName ORDER_TEXT = QName.createQName(TEST_NAMESPACE, "orderText");
    QName ORDER_LOCALISED_TEXT = QName.createQName(TEST_NAMESPACE, "orderLocalisedText");
    QName ORDER_ML_TEXT = QName.createQName(TEST_NAMESPACE, "orderMLText");
    QName ORDER_LOCALISED_ML_TEXT = QName.createQName(TEST_NAMESPACE, "orderLocalisedMLText");
    QName TEST_SUPER_TYPE = QName.createQName(TEST_NAMESPACE, "testSuperType");
    QName TEST_TYPE = QName.createQName(TEST_NAMESPACE, "testType");
    QName TEST_ASPECT = QName.createQName(TEST_NAMESPACE, "testAspect");
    QName extendedContent = QName.createQName(CMIS_TEST_NAMESPACE, "extendedContent");
    QName SINGLE_TEXT_BOTH = QName.createQName(CMIS_TEST_NAMESPACE, "singleTextBoth");
    QName SINGLE_TEXT_UNTOKENISED = QName.createQName(CMIS_TEST_NAMESPACE, "singleTextUntokenised");
    QName SINGLE_TEXT_TOKENISED = QName.createQName(CMIS_TEST_NAMESPACE, "singleTextTokenised");
    QName MULTIPLE_TEXT_BOTH = QName.createQName(CMIS_TEST_NAMESPACE, "multipleTextBoth");
    QName MULTIPLE_TEXT_UNTOKENISED = QName.createQName(CMIS_TEST_NAMESPACE, "multipleTextUntokenised");
    QName MULTIPLE_TEXT_TOKENISED = QName.createQName(CMIS_TEST_NAMESPACE, "multipleTextTokenised");
    QName SINGLE_ML_TEXT_BOTH = QName.createQName(CMIS_TEST_NAMESPACE, "singleMLTextBoth");
    QName SINGLE_ML_TEXT_UNTOKENISED = QName.createQName(CMIS_TEST_NAMESPACE, "singleMLTextUntokenised");
    QName SINGLE_ML_TEXT_TOKENISED = QName.createQName(CMIS_TEST_NAMESPACE, "singleMLTextTokenised");
    QName MULTIPLE_ML_TEXT_BOTH = QName.createQName(CMIS_TEST_NAMESPACE, "multipleMLTextBoth");
    QName MULTIPLE_ML_TEXT_UNTOKENISED = QName.createQName(CMIS_TEST_NAMESPACE, "multipleMLTextUntokenised");
    QName MULTIPLE_ML_TEXT_TOKENISED = QName.createQName(CMIS_TEST_NAMESPACE, "multipleMLTextTokenised");
    QName SINGLE_FLOAT = QName.createQName(CMIS_TEST_NAMESPACE, "singleFloat");
    QName MULTIPLE_FLOAT = QName.createQName(CMIS_TEST_NAMESPACE, "multipleFloat");
    QName SINGLE_DOUBLE = QName.createQName(CMIS_TEST_NAMESPACE, "singleDouble");
    QName MULTIPLE_DOUBLE = QName.createQName(CMIS_TEST_NAMESPACE, "multipleDouble");
    QName SINGLE_INTEGER = QName.createQName(CMIS_TEST_NAMESPACE, "singleInteger");
    QName MULTIPLE_INTEGER = QName.createQName(CMIS_TEST_NAMESPACE, "multipleInteger");
    QName SINGLE_LONG = QName.createQName(CMIS_TEST_NAMESPACE, "singleLong");
    QName MULTIPLE_LONG = QName.createQName(CMIS_TEST_NAMESPACE, "multipleLong");
    QName SINGLE_BOOLEAN = QName.createQName(CMIS_TEST_NAMESPACE, "singleBoolean");
    QName MULTIPLE_BOOLEAN = QName.createQName(CMIS_TEST_NAMESPACE, "multipleBoolean");
    QName SINGLE_DATE = QName.createQName(CMIS_TEST_NAMESPACE, "singleDate");
    QName MULTIPLE_DATE = QName.createQName(CMIS_TEST_NAMESPACE, "multipleDate");
    QName SINGLE_DATETIME = QName.createQName(CMIS_TEST_NAMESPACE, "singleDatetime");
    QName MULTIPLE_DATETIME = QName.createQName(CMIS_TEST_NAMESPACE, "multipleDatetime");
}