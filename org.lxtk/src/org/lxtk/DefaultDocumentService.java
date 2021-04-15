/*******************************************************************************
 * Copyright (c) 2019, 2021 1C-Soft LLC.
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
    private final EventEmitter<TextDocumentChangeEvent> onWillChangeTextDocument =
        new EventEmitter<>();
    private final EventEmitter<TextDocumentChangeEvent> onDidChangeTextDocument =
        new EventEmitter<>();

    @Override
    public Disposable addTextDocument(TextDocument document)
    {
        URI uri = normalize(document.getUri());
        if (textDocuments.putIfAbsent(uri, document) != null)
        {
            throw new IllegalArgumentException(
                "The service already manages a text document with URI " + uri); //$NON-NLS-1$
        }
        Disposable result = SafeRun.runWithResult(rollback ->
        {
            // Must subscribe to document change events before onDidAddTextDocument is fired.
            // Otherwise, it would be possible to lose some change events in a blindspot.
            EventStream<TextDocumentChangeEvent> onWillChange = document.onWillChange();
            if (onWillChange != null)
            {
                Disposable willChangeSubscription = onWillChange.subscribe(
                    event -> onWillChangeTextDocument.fire(event, getLogger()));
                rollback.add(willChangeSubscription::dispose);
            }
            Disposable didChangeSubscription = document.onDidChange().subscribe(
                event -> onDidChangeTextDocument.fire(event, getLogger()));
            rollback.add(didChangeSubscription::dispose);

            onDidAddTextDocument.fire(document, getLogger());
            rollback.add(() ->
            {
                if (textDocuments.remove(uri, document))
                    onDidRemoveTextDocument.fire(document, getLogger());
            });

            rollback.setLogger(getLogger());
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
    public EventStream<TextDocumentChangeEvent> onWillChangeTextDocument()
    {
        return onWillChangeTextDocument;
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

    /**
     * Returns an exception logger for this service.
     *
     * @return a logger instance (may be <code>null</code>)
     */
    protected Consumer<Throwable> getLogger()
    {
        return null;
    }
}
