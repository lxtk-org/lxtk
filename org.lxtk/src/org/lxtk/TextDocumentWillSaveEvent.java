/*******************************************************************************
 * Copyright (c) 2021 1C-Soft LLC.
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

import java.util.Objects;

import org.eclipse.lsp4j.TextDocumentSaveReason;

/**
 * An event describing an upcoming save of a {@link TextDocument}.
 */
public class TextDocumentWillSaveEvent
{
    private final TextDocument document;
    private final TextDocumentSaveReason reason;

    /**
     * Constructor.
     *
     * @param document not <code>null</code>
     * @param reason not <code>null</code>
     */
    public TextDocumentWillSaveEvent(TextDocument document, TextDocumentSaveReason reason)
    {
        this.document = Objects.requireNonNull(document);
        this.reason = Objects.requireNonNull(reason);
    }

    /**
     * Return the document that is going to be saved.
     *
     * @return the document to be saved (never <code>null</code>)
     */
    public final TextDocument getDocument()
    {
        return document;
    }

    /**
     * Returns the reason why the document is saved.
     *
     * @return the save reason (never <code>null</code>)
     */
    public final TextDocumentSaveReason getReason()
    {
        return reason;
    }
}
