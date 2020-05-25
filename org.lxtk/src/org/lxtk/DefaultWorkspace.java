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
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import org.lxtk.util.Disposable;
import org.lxtk.util.EventEmitter;
import org.lxtk.util.EventStream;
import org.lxtk.util.UriUtil;
import org.lxtk.util.UriUtil.Normalization;

/**
 * Default implementation of the {@link Workspace} interface.
 * <p>
 * This implementation is thread-safe.
 * </p>
 */
public class DefaultWorkspace
    implements Workspace
{
    private final Map<URI, TextDocument> textDocuments = new ConcurrentHashMap<>();
    private final EventEmitter<TextDocument> onDidAddTextDocument = new EventEmitter<>();
    private final EventEmitter<TextDocument> onDidRemoveTextDocument = new EventEmitter<>();
    private final EventEmitter<TextDocumentChangeEvent> onDidChangeTextDocument =
        new EventEmitter<>();
    private final Consumer<TextDocumentChangeEvent> textDocumentChangeEventConsumer =
        event -> onDidChangeTextDocument.fire(event);

    @Override
    public Disposable addTextDocument(TextDocument document)
    {
        URI uri = normalize(document.getUri());
        if (textDocuments.putIfAbsent(uri, document) != null)
        {
            throw new IllegalArgumentException("The workspace already contains a document with URI " //$NON-NLS-1$
                + uri);
        }
        Disposable subscription = document.onDidChange().subscribe(textDocumentChangeEventConsumer);
        onDidAddTextDocument.fire(document);
        return () ->
        {
            subscription.dispose();
            if (textDocuments.remove(document.getUri(), document))
                onDidRemoveTextDocument.fire(document);
        };
    }

    @Override
    public Collection<TextDocument> getTextDocuments()
    {
        return Collections.unmodifiableCollection(textDocuments.values());
    }

    @Override
    public TextDocument getTextDocument(URI uri)
    {
        if (uri == null)
            return null;
        return textDocuments.get(normalize(uri));
    }

    @Override
    public EventStream<TextDocument> onDidAddTextDocument()
    {
        return onDidAddTextDocument;
    }

    @Override
    public EventStream<TextDocument> onDidRemoveTextDocument()
    {
        return onDidRemoveTextDocument;
    }

    @Override
    public EventStream<TextDocumentChangeEvent> onDidChangeTextDocument()
    {
        return onDidChangeTextDocument;
    }

    /**
     * Normalizes the given URI.
     *
     * @param uri never <code>null</code>
     * @return the normalized URI (not <code>null</code>)
     */
    protected URI normalize(URI uri)
    {
        return UriUtil.normalize(uri, EnumSet.of(Normalization.ENCODING, Normalization.PATH));
    }
}
