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

import java.net.URI;
import java.util.Collections;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentExtension4;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextDocumentContentChangeEvent;
import org.lxtk.TextDocument;
import org.lxtk.TextDocumentChangeEvent;
import org.lxtk.TextDocumentSnapshot;
import org.lxtk.util.Disposable;
import org.lxtk.util.EventEmitter;
import org.lxtk.util.EventStream;

/**
 * TODO JavaDoc
 */
public final class EclipseTextDocument
    implements TextDocument, Disposable
{
    private final URI uri;
    private final String languageId;
    private final IDocument document;
    private final AtomicReference<EclipseTextDocumentChangeEvent> lastChange =
        new AtomicReference<>();
    private final EventEmitter<TextDocumentChangeEvent> onDidChange =
        new EventEmitter<>();
    private final IDocumentListener listener = new IDocumentListener()
    {
        private TextDocumentContentChangeEvent contentChange;

        @Override
        public void documentAboutToBeChanged(DocumentEvent event)
        {
            contentChange = newContentChangeEvent(event);
        }

        @Override
        public void documentChanged(DocumentEvent event)
        {
            try
            {
                notifyChange(newChangeEvent(contentChange,
                    event.getModificationStamp()));
            }
            finally
            {
                contentChange = null;
            }
        }
    };

    /**
     * TODO JavaDoc
     *
     * @param uri not <code>null</code>
     * @param languageId not <code>null</code>
     * @param document not <code>null</code>
     */
    public EclipseTextDocument(URI uri, String languageId, IDocument document)
    {
        this.uri = Objects.requireNonNull(uri);
        this.languageId = Objects.requireNonNull(languageId);
        this.document = Objects.requireNonNull(document);
        document.addDocumentListener(listener);
        lastChange.compareAndSet(null, new EclipseTextDocumentChangeEvent(
            new TextDocumentSnapshot(this, 0, document.get()),
            Collections.emptyList(), getModificationStamp()));
    }

    /**
     * TODO JavaDoc
     *
     * @return the underlying {@link IDocument} (never <code>null</code>)
     */
    public IDocument getUnderlyingDocument()
    {
        return document;
    }

    /**
     * TODO JavaDoc
     *
     * @return the modification stamp of the underlying {@link IDocument}
     *  or <code>UNKNOWN_MODIFICATION_STAMP</code>
     * @see IDocumentExtension4#getModificationStamp()
     */
    public long getModificationStamp()
    {
        if (document instanceof IDocumentExtension4)
            return ((IDocumentExtension4)document).getModificationStamp();
        return IDocumentExtension4.UNKNOWN_MODIFICATION_STAMP;
    }

    @Override
    public void dispose()
    {
        document.removeDocumentListener(listener);
        onDidChange.dispose();
    }

    @Override
    public URI getUri()
    {
        return uri;
    }

    @Override
    public String getLanguageId()
    {
        return languageId;
    }

    @Override
    public EclipseTextDocumentChangeEvent getLastChange()
    {
        return lastChange.get();
    }

    @Override
    public EventStream<TextDocumentChangeEvent> onDidChange()
    {
        return onDidChange;
    }

    private void notifyChange(EclipseTextDocumentChangeEvent event)
    {
        onDidChange.fire(event);
        lastChange.set(event);
    }

    private TextDocumentContentChangeEvent newContentChangeEvent(
        DocumentEvent event)
    {
        Range range;
        try
        {
            range = DocumentUtil.toRange(document, event.getOffset(),
                event.getLength());
        }
        catch (BadLocationException e)
        {
            throw new AssertionError(e); // must never happen
        }
        return new TextDocumentContentChangeEvent(range, event.getLength(),
            event.getText());
    }

    private EclipseTextDocumentChangeEvent newChangeEvent(
        TextDocumentContentChangeEvent event, long modificationStamp)
    {
        TextDocumentChangeEvent lastEvent = lastChange.get();
        int lastVersion = (lastEvent == null) ? 0
            : lastEvent.getSnapshot().getVersion();
        TextDocumentSnapshot snapshot = new TextDocumentSnapshot(this,
            lastVersion + 1, document.get());
        if (modificationStamp != getModificationStamp())
            throw new AssertionError();
        return new EclipseTextDocumentChangeEvent(snapshot,
            Collections.singletonList(event), modificationStamp);
    }
}
