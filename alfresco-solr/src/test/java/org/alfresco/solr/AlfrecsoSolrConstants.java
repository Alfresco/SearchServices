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
    String TEST_NAMESPACE = "http://www.alfresco.org/test/solrtest";
    QName createdDate = QName.createQName(TEST_NAMESPACE, "createdDate");
    QName createdTime = QName.createQName(TEST_NAMESPACE, "createdTime");
    QName orderDouble = QName.createQName(TEST_NAMESPACE, "orderDouble");
    QName orderFloat = QName.createQName(TEST_NAMESPACE, "orderFloat");
    QName orderLong = QName.createQName(TEST_NAMESPACE, "orderLong");
    QName orderInt = QName.createQName(TEST_NAMESPACE, "orderInt");
    QName orderText = QName.createQName(TEST_NAMESPACE, "orderText");
    QName orderLocalisedText = QName.createQName(TEST_NAMESPACE, "orderLocalisedText");
    QName orderMLText = QName.createQName(TEST_NAMESPACE, "orderMLText");
    QName orderLocalisedMLText = QName.createQName(TEST_NAMESPACE, "orderLocalisedMLText");
    QName testSuperType = QName.createQName(TEST_NAMESPACE, "testSuperType");
    QName testType = QName.createQName(TEST_NAMESPACE, "testType");
    QName testAspect = QName.createQName(TEST_NAMESPACE, "testAspect");
}
