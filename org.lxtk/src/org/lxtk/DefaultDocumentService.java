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

import org.lxtk.util.Disposable;
import org.lxtk.util.EventEmitter;
import org.lxtk.util.EventStream;
import org.lxtk.util.SafeRun;
import org.lxtk.util.UriUtil;
import org.lxtk.util.UriUtil.Normalization;

/**
 * Default implementation of the {@link DocumentService} interface.
 * <p>
 * This implementation is thread-safe.
 * </p>
 */
public class DefaultDocumentService
    implements DocumentService
{
    private final Map<URI, TextDocument> textDocuments = new ConcurrentHashMap<>();
    private final EventEmitter<TextDocument> onDidAddTextDocument = new EventEmitter<>();
    private final EventEmitter<TextDocument> onDidRemoveTextDocument = new EventEmitter<>();
    private final EventEmitter<TextDocumentChangeEvent> onDidChangeTextDocument =
        new EventEmitter<>();
    private final EventEmitter<TextDocumentSaveEvent> onDidSaveTextDocument = new EventEmitter<>();

    @Override
    public Disposable addTextDocument(TextDocument document)
    {
        URI uri = normalize(document.getUri());
        if (textDocuments.putIfAbsent(uri, document) != null)
        {
            throw new IllegalArgumentException("The service already manages a text document with URI " //$NON-NLS-1$
                + uri);
        }
        Disposable result = SafeRun.runWithResult(rollback ->
        {
            // Must subscribe to the document's didChange before onDidAddTextDocument is fired.
            // Otherwise, it would be possible to lose some change events in a blindspot.
            Disposable didChangeSubscription =
                document.onDidChange().subscribe(event -> onDidChangeTextDocument.fire(event));
            rollback.add(didChangeSubscription::dispose);

            onDidAddTextDocument.fire(document);
            rollback.add(() ->
            {
                if (textDocuments.remove(uri, document))
                    onDidRemoveTextDocument.fire(document);
            });

            Disposable didSaveSubscription =
                document.onDidSave().subscribe(event -> onDidSaveTextDocument.fire(event));
            rollback.add(didSaveSubscription::dispose);

            rollback.setLogger(e -> e.printStackTrace());
            return rollback::run;
        });
        return result;
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

    @Override
    public EventStream<TextDocumentSaveEvent> onDidSaveTextDocument()
    {
        return onDidSaveTextDocument;
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
