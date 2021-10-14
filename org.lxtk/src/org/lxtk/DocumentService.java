/*******************************************************************************
 * Copyright (c) 2019, 2021 1C-Soft LLC.
 *
 * This program and the accompanying materials are made available under
 * the terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Vladimir Piskarev (1C) - initial API and implementation
 *******************************************************************************/
package org.lxtk;

import java.net.URI;
import java.util.Collection;

import org.lxtk.util.Disposable;
import org.lxtk.util.EventStream;

/**
 * Provides support for document management.
 *
 * @see DefaultDocumentService
 */
public interface DocumentService
{
    /**
     * Returns the default document matcher for this service.
     *
     * @return the default document matcher (never <code>null</code>)
     */
    default DocumentMatcher getDocumentMatcher()
    {
        return DefaultDocumentMatcher.INSTANCE;
    }

    /**
     * Adds a text document to the collection of documents managed by this service.
     * <p>
     * If the service already manages a text document with an equivalent URI,
     * a runtime exception is thrown.
     * </p>
     *
     * @param document not <code>null</code>
     * @return a disposable to remove the document from the collection of documents
     *  managed by this service (never <code>null</code>)
     */
    Disposable addTextDocument(TextDocument document);

    /**
     * Returns all text documents currently managed by this service.
     * <p>
     * This method must not try to acquire any kind of lock that might conflict
     * with any locks held during didAddTextDocument, didRemoveTextDocument,
     * willChangeTextDocument, or didChangeTextDocument event notification.
     * </p>
     *
     * @return all text documents currently managed by the service
     *  (never <code>null</code>, may be empty). Clients <b>must not</b>
     *  modify the returned collection
     */
    Collection<TextDocument> getTextDocuments();

    /**
     * Returns the text document that is managed by this service and
     * has the URI equivalent to the given URI.
     * <p>
     * This method must not try to acquire any kind of lock that might conflict
     * with any locks held during didAddTextDocument, didRemoveTextDocument,
     * willChangeTextDocument, or didChangeTextDocument event notification.
     * </p>
     *
     * @param uri may be <code>null</code>, in which case <code>null</code>
     *  is returned
     * @return the corresponding text document, or <code>null</code> if none
     */
    TextDocument getTextDocument(URI uri);

    /**
     * Returns a stream of events that are emitted when a text document is added
     * to the collection of documents managed by this service.
     *
     * @return a stream of events that are emitted when a text document is added
     *  (never <code>null</code>)
     */
    EventStream<TextDocument> onDidAddTextDocument();

    /**
     * Returns a stream of events that are emitted when a text document is removed
     * from the collection of documents managed by this service.
     *
     * @return a stream of events that are emitted when a text document is removed
     *  (never <code>null</code>)
     */
    EventStream<TextDocument> onDidRemoveTextDocument();

    /**
     * Returns a stream of events that are emitted when the content of a managed text document
     * is about to be changed. Note that an event will be emitted if and only if
     * it is {@link TextDocument#onWillChange() supported} by the text document.
     * <p>
     * These events may be emitted even before firing {@link #onDidAddTextDocument()}
     * or after firing {@link #onDidRemoveTextDocument()} for a text document.
     * This provision is to avoid a possibility of a blind-spot where some
     * change events might have escaped a client due to a race condition.
     * </p>
     *
     * @return a stream of events that are emitted when the content of a managed text document
     *  is about to be changed (never <code>null</code>)
     */
    EventStream<TextDocumentChangeEvent> onWillChangeTextDocument();

    /**
     * Returns a stream of events that are emitted when the content of a managed text document
     * changes.
     * <p>
     * These events may be emitted even before firing {@link #onDidAddTextDocument()}
     * or after firing {@link #onDidRemoveTextDocument()} for a text document.
     * This provision is to avoid a possibility of a blind-spot where some
     * change events might have escaped a client due to a race condition.
     * </p>
     *
     * @return a stream of events that are emitted when the content of a managed text document
     *  changes (never <code>null</code>)
     */
    EventStream<TextDocumentChangeEvent> onDidChangeTextDocument();
}
