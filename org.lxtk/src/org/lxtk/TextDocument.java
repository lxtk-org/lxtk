/*******************************************************************************
 * Copyright (c) 2019, 2020 1C-Soft LLC.
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

import org.lxtk.util.EventStream;

/**
 * Represents a text document, such as a source file.
 *
 * @see DocumentService
 */
public interface TextDocument
{
    /**
     * Returns the document's URI. The document's URI does not change over the
     * lifetime of the document.
     *
     * @return the document's URI (never <code>null</code>)
     */
    URI getUri();

    /**
     * Returns the document's language. The document's language does not change
     * over the lifetime of the document.
     *
     * @return the document's language (never <code>null</code>)
     */
    String getLanguageId();

    /**
     * Returns an event describing the last reported change of the document.
     * If no change has been reported yet, the event will describe the initial
     * content of the document.
     *
     * @return an event describing the last reported change of the document
     *  (never <code>null</code>)
     */
    TextDocumentChangeEvent getLastChange();

    /**
     * Returns an event emitter firing when the content of the document changes. 
     *
     * @return an event emitter firing when the content of the document changes
     *  (never <code>null</code>)
     */
    EventStream<TextDocumentChangeEvent> onDidChange();

    /**
     * Returns an event emitter firing when the document is saved.
     *
     * @return an event emitter firing when the document is saved
     *  (never <code>null</code>)
     */
    EventStream<TextDocumentSaveEvent> onDidSave();
}
