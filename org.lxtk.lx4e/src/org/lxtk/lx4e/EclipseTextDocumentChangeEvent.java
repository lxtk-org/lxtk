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
package org.lxtk.lx4e;

import java.util.List;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentExtension4;
import org.eclipse.lsp4j.TextDocumentContentChangeEvent;
import org.lxtk.TextDocumentChangeEvent;
import org.lxtk.TextDocumentSnapshot;

/**
 * TODO JavaDoc
 */
public final class EclipseTextDocumentChangeEvent
    extends TextDocumentChangeEvent
{
    private final long modificationStamp;

    EclipseTextDocumentChangeEvent(TextDocumentSnapshot snapshot,
        List<TextDocumentContentChangeEvent> contentChanges,
        long modificationStamp)
    {
        super(snapshot, contentChanges);
        this.modificationStamp = modificationStamp;
    }

    /**
     * TODO JavaDoc
     *
     * @return the underlying {@link IDocument} (never <code>null</code>)
     */
    public IDocument getUnderlyingDocument()
    {
        return ((EclipseTextDocument)getDocument()).getUnderlyingDocument();
    }

    /**
     * TODO JavaDoc
     *
     * @return the modification stamp of the underlying {@link IDocument} at the
     *  time when this event was sent or <code>UNKNOWN_MODIFICATION_STAMP</code>
     * @see IDocumentExtension4#getModificationStamp()
     */
    public long getModificationStamp()
    {
        return modificationStamp;
    }
}
