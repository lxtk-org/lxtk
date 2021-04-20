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

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import org.eclipse.lsp4j.TextDocumentSaveReason;
import org.eclipse.lsp4j.TextEdit;
import org.lxtk.util.DisposableObject;

/**
 * An event describing an upcoming save of a {@link TextDocument}.
 */
public class TextDocumentWillSaveEvent
    extends DisposableObject
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

    /**
     * Allows to apply the text edits of the given future to the document before it is saved.
     * Edits of subsequent calls to this method will be applied in order. The edits will be ignored
     * if a concurrent modification of the document happened in the meantime.
     * <p>
     * The default implementation of this method throws {@link UnsupportedOperationException}.
     * Subclasses may override.
     * </p>
     * <p>
     * <b>Note:</b> This method may only be called in the dynamic scope of the event notification.
     * </p>
     *
     * @param future not <code>null</code>. Note that the result of the future may be <code>null</code>
     * @see TextDocumentWillSaveEventSource#supportsWaitUntil()
     */
    public void waitUntil(CompletableFuture<List<TextEdit>> future)
    {
        throw new UnsupportedOperationException();
    }
}
