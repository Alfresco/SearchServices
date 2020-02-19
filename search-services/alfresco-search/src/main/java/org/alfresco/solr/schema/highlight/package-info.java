/*
 * Copyright (C) 2005-2020 Alfresco Software Limited.
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
/**
 * This package contains a custom field type (and dependent component) which is supposed to be used
 * only on fields that
 *
 * <ul>
 *     <li>are marked as stored and not indexed (indexed = false, stored = true)</li>
 *     <li>are used in highlighting requests (the reason why this package belongs to a "highlight" namespace)</li>
 * </ul>
 *
 * The underlying reason of this customisation is mainly related with the custom highlighter used in Alfresco Search
 * Services: highlight fields needs have the following requirements:
 *
 * <ul>
 *     <li>they have to be stored</li>
 *     <li>
 *         they don't have to be indexed but they must have a TextField (or a subclass) as type, because they must
 *         provide an index time  {@link org.apache.lucene.analysis.Analyzer} (yes, even if indexed is set to false)
 *         which will be used for analysing the stored content and extract the highlighting snippets.
 *     </li>
 * </ul>
 *
 * The field type purpose is actually to define a custom analyzer which is able to detect the proper localised analyzer
 * at runtime, depending on the locale marker prefix put on the stored content.
 * For example,
 *
 * <ul>
 *     <li>
 *         a field "title" with the following content: "\u0000en\u0000this is an english title" will be highlighted
 *         using an english analyzer (specifically the index analyzer of the "highlighted_text_en" field type, or
 *         the index analyzer of the "text_en" field type in case the previous one is missing)
 *     </li>
 *     <li>
 *         a field "title" with the following content: "\u0000it\u0000Questo sarebbe un titolo" will be highlighted
 *         using an italian analyzer (specifically the index analyzer of the "highlighted_text_it" field type, or
 *         the index analyzer of the "text_it" field type in case the previous one is missing)
 *     </li>
 *     <li>
 *         a field "title" with the following content: "This is a title without any locale marker" will be highlighted
 *         using the analyzer of the general text field "text___".
 *     </li>
 *     <li>
 *         a field "title" with the following content: "\u0000unknown_locale\u0000This is a title without an unknown locale marker"
 *         will be highlighted using the analyzer of the general text field "text___".
 *     </li>
 * </ul>
 *
 *
 */
package org.alfresco.solr.schema.highlight;