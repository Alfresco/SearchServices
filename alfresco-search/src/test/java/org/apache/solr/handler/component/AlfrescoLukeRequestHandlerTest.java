/*
 * Copyright (C) 2005-2013 Alfresco Software Limited.
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
package org.apache.solr.handler.component;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.io.IOException;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.LeafReader;
import org.apache.lucene.index.PostingsEnum;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.util.Bits;
import org.apache.lucene.util.BytesRef;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mock;

/**
 * Unit tests for the {@link AlfrescoLukeRequestHandler}.
 */
public class AlfrescoLukeRequestHandlerTest
{
    /** The reference to text for a search term. */
    private static final BytesRef TERM_TEXT = new BytesRef("TermText");
    /** A document id. */
    private static final int DOC_ID = 123;

    @Mock
    private Terms mockTerms;
    @Mock
    private TermsEnum mockTermsEnum;
    @Mock
    private PostingsEnum mockPostingsEnum;
    @Mock
    private LeafReader mockReader;
    @Mock
    private Bits mockBits;

    @Before
    public void setUp() throws IOException
    {
        initMocks(this);

        // Link up the mocks.
        when(mockTerms.iterator()).thenReturn(mockTermsEnum);
        when(mockTermsEnum.postings(null, PostingsEnum.NONE)).thenReturn(mockPostingsEnum);
    }

    /** Check that we can find a live document for a set of search terms. */
    @Test
    public void testFindLiveDoc() throws IOException
    {
        // Set up the term to return the document.
        when(mockTermsEnum.next()).thenReturn(TERM_TEXT);
        when(mockPostingsEnum.nextDoc()).thenReturn(DOC_ID);
        when(mockPostingsEnum.docID()).thenReturn(DOC_ID);
        // Set up the leaf reader to be able to return a document.
        when(mockReader.getLiveDocs()).thenReturn(mockBits);
        Document document = new Document();
        when(mockReader.document(DOC_ID)).thenReturn(document);

        // Call the method under test.
        Document firstLiveDoc = AlfrescoLukeRequestHandler.getFirstLiveDoc(mockTerms, mockReader);

        // We need to ensure that the PostingsEnum is initialised before use by a call to nextDoc.
        InOrder postingsInitialisedCheck = inOrder(mockPostingsEnum);
        postingsInitialisedCheck.verify(mockPostingsEnum).nextDoc();
        postingsInitialisedCheck.verify(mockPostingsEnum, atLeastOnce()).docID();
        // Check the returned value.
        assertEquals("Expected to find the document.", document, firstLiveDoc);
    }

    /** Check that we can find a live document for a set of search terms when no documents have been deleted. */
    @Test
    public void testFindLiveDocWithNoDeletedDocuments() throws IOException
    {
        // Set up the term to return the document.
        when(mockTermsEnum.next()).thenReturn(TERM_TEXT);
        when(mockPostingsEnum.nextDoc()).thenReturn(DOC_ID);
        when(mockPostingsEnum.docID()).thenReturn(DOC_ID);
        // Returning null indicates that there are no deleted documents.
        when(mockReader.getLiveDocs()).thenReturn(null);
        Document document = new Document();
        when(mockReader.document(DOC_ID)).thenReturn(document);

        // Call the method under test.
        Document firstLiveDoc = AlfrescoLukeRequestHandler.getFirstLiveDoc(mockTerms, mockReader);

        // We need to ensure that the PostingsEnum is initialised before use by a call to nextDoc.
        InOrder postingsInitialisedCheck = inOrder(mockPostingsEnum);
        postingsInitialisedCheck.verify(mockPostingsEnum).nextDoc();
        postingsInitialisedCheck.verify(mockPostingsEnum, atLeastOnce()).docID();
        // Check the returned value.
        assertEquals("Expected to find the document.", document, firstLiveDoc);
    }

    /** Check that we can find a live document for a set of search terms when no documents have been deleted. */
    @Test
    public void testFindLiveDocWithNullTerm() throws IOException
    {
        // There are no search terms.
        when(mockTermsEnum.next()).thenReturn(null);

        // Call the method under test.
        Document firstLiveDoc = AlfrescoLukeRequestHandler.getFirstLiveDoc(mockTerms, mockReader);

        // Check the returned value.
        assertNull("Expected no document to be returned.", firstLiveDoc);
    }

    /** Check the behaviour when the first document found was deleted. */
    @Test
    public void testDocWasDeleted() throws IOException
    {
        // Set up the term to return the document.
        when(mockTermsEnum.next()).thenReturn(TERM_TEXT);
        when(mockPostingsEnum.nextDoc()).thenReturn(DOC_ID);
        when(mockPostingsEnum.docID()).thenReturn(DOC_ID);
        // Set up the leaf reader to return a deleted document.
        when(mockReader.getLiveDocs()).thenReturn(mockBits);
        Document document = new Document();
        when(mockReader.document(DOC_ID)).thenReturn(document);
        when(mockBits.get(DOC_ID)).thenReturn(true);

        // Call the method under test.
        Document firstLiveDoc = AlfrescoLukeRequestHandler.getFirstLiveDoc(mockTerms, mockReader);

        // Check the returned value.
        assertNull("Expected no document to be returned.", firstLiveDoc);
    }

    /** Check the behaviour when there are no more documents matching the terms. */
    @Test
    public void testNoMoreDocs() throws IOException
    {
        // There is a search term but no matching documents.
        when(mockTermsEnum.next()).thenReturn(TERM_TEXT);
        when(mockPostingsEnum.nextDoc()).thenReturn(DocIdSetIterator.NO_MORE_DOCS);

        // Call the method under test.
        Document firstLiveDoc = AlfrescoLukeRequestHandler.getFirstLiveDoc(mockTerms, mockReader);

        // Check the returned value.
        assertNull("Expected no document to be returned.", firstLiveDoc);
    }
}
