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

import java.util.List;
import java.util.Objects;

import org.eclipse.lsp4j.TextDocumentContentChangeEvent;

/**
 * An event describing a change of a {@link TextDocument}.
 */
public class TextDocumentChangeEvent
{
    private final TextDocumentSnapshot snapshot;
    private final List<TextDocumentContentChangeEvent> contentChanges;

    /**
     * Constructor.
     *
     * @param snapshot the current snapshot of the changed document
     *  (not <code>null</code>)
     * @param contentChanges the actual content changes (not <code>null</code>)
     */
    public TextDocumentChangeEvent(TextDocumentSnapshot snapshot,
        List<TextDocumentContentChangeEvent> contentChanges)
    {
        this.snapshot = Objects.requireNonNull(snapshot);
        this.contentChanges = Objects.requireNonNull(contentChanges);
    }

    /**
     * Returns the changed document.
     *
     * @return the changed document (never <code>null</code>)
     */
    public final TextDocument getDocument()
    {
        return snapshot.getDocument();
    }

    /**
     * Returns the snapshot of the document at the time when this event
     * was sent.
     *
     * @return the snapshot of the document at the time when this event
     *  was sent (never <code>null</code>)
     */
    public final TextDocumentSnapshot getSnapshot()
    {
        return snapshot;
    }

    /**
     * Returns the actual content changes.
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
