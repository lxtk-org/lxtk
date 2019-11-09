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

import java.util.List;
import java.util.Objects;

import org.eclipse.lsp4j.TextDocumentContentChangeEvent;

/**
 * TODO JavaDoc
 */
public class TextDocumentChangeEvent
{
    private final TextDocumentSnapshot snapshot;
    private final List<TextDocumentContentChangeEvent> contentChanges;

    /**
     * TODO JavaDoc
     *
     * @param snapshot not <code>null</code>
     * @param contentChanges not <code>null</code>
     */
    public TextDocumentChangeEvent(TextDocumentSnapshot snapshot,
        List<TextDocumentContentChangeEvent> contentChanges)
    {
        this.snapshot = Objects.requireNonNull(snapshot);
        this.contentChanges = Objects.requireNonNull(contentChanges);
    }

    /**
     * TODO JavaDoc
     *
     * @return the changed document (never <code>null</code>)
     */
    public final TextDocument getDocument()
    {
        return snapshot.getDocument();
    }

    /**
     * TODO JavaDoc
     *
     * @return the snapshot of the document at the time when this event
     *  was sent (never <code>null</code>)
     */
    public final TextDocumentSnapshot getSnapshot()
    {
        return snapshot;
    }

    /**
     * TODO JavaDoc
     *
     * @return the actual content changes (never <code>null</code>,
     *  may be empty). Clients <b>must not</b> modify the returned list
     *  or any of the contained objects.
     */
    public final List<TextDocumentContentChangeEvent> getContentChanges()
    {
        return contentChanges;
    }
}
