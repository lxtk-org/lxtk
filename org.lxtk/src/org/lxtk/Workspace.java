/*******************************************************************************
 * Copyright (c) 2019 1C-Soft LLC.
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
 * TODO JavaDoc
 */
public interface Workspace
{
    /**
     * TODO JavaDoc
     *
     * @return a document matcher (never <code>null</code>)
     */
    default DocumentMatcher getDocumentMatcher()
    {
        return DefaultDocumentMatcher.INSTANCE;
    }

    /**
     * TODO JavaDoc
     * <p>
     * If the workspace already contains a document with an equivalent URI,
     * an exception will be thrown.
     * </p>
     *
     * @param document not <code>null</code>
     * @return a disposable to remove the added text document
     *  (never <code>null</code>)
     */
    Disposable addTextDocument(TextDocument document);

    /**
     * TODO JavaDoc
     * <p>
     * This method must not try to obtain any kind of lock that might conflict
     * with any locks held while firing didAddTextDocument, didRemoveTextDocument,
     * or didChangeTextDocument events.
     * </p>
     *
     * @return all text documents currently known to the workspace
     *  (never <code>null</code>, may be empty)
     */
    Collection<TextDocument> getTextDocuments();

    /**
     * TODO JavaDoc
     *
     * @param uri may be <code>null</code>, in which case <code>null</code>
     *  will be returned
     * @return the corresponding text document in the workspace,
     *  or <code>null</code> if none
     */
    TextDocument getTextDocument(URI uri);

    /**
     * TODO JavaDoc
     *
     * @return an event emitter firing when a text document is added
     *  (never <code>null</code>)
     */
    EventStream<TextDocument> onDidAddTextDocument();

    /**
     * TODO JavaDoc
     *
     * @return an event emitter firing when a text document is removed
     *  (never <code>null</code>)
     */
    EventStream<TextDocument> onDidRemoveTextDocument();

    /**
     * TODO JavaDoc
     *
     * @return an event emitter firing when the content of a text document changes
     *  (never <code>null</code>)
     */
    EventStream<TextDocumentChangeEvent> onDidChangeTextDocument();
}
