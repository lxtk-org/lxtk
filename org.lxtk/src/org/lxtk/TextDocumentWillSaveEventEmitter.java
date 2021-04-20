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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import org.eclipse.lsp4j.TextDocumentSaveReason;
import org.eclipse.lsp4j.TextEdit;
import org.lxtk.util.Disposable;
import org.lxtk.util.EventEmitter;
import org.lxtk.util.EventStream;

/**
 * An emitter of {@link TextDocumentWillSaveEvent}s with
 * {@link TextDocumentWillSaveEvent#waitUntil(CompletableFuture) waitUntil} support.
 */
public class TextDocumentWillSaveEventEmitter
    implements EventStream<TextDocumentWillSaveEvent>, Disposable
{
    private EventEmitter<TextDocumentWillSaveEvent> delegate = new EventEmitter<>();

    /**
     * Notify all subscribers about a {@link TextDocumentWillSaveEvent}; the given exception handler
     * is used to handle any exception thrown by an event consumer.
     *
     * @param document not <code>null</code>
     * @param reason not <code>null</code>
     * @param exceptionHandler may be <code>null</code>, in which case
     *  any exception thrown by an event consumer is suppressed
     * @return a future that is completed when all of the futures passed by event consumers to
     *  the event's {@link TextDocumentWillSaveEvent#waitUntil(CompletableFuture) waitUntil} method
     *  complete (never <code>null</code>)
     */
    public CompletableFuture<List<List<TextEdit>>> fire(TextDocument document,
        TextDocumentSaveReason reason, Consumer<Throwable> exceptionHandler)
    {
        List<CompletableFuture<List<TextEdit>>> futures = new ArrayList<>();
        TextDocumentWillSaveEvent event = new TextDocumentWillSaveEvent(document, reason)
        {
            @Override
            public void waitUntil(CompletableFuture<List<TextEdit>> future)
            {
                if (isDisposed())
                    throw new IllegalStateException("The event object has been disposed"); //$NON-NLS-1$

                futures.add(Objects.requireNonNull(future));
            }
        };
        delegate.fire(event, exceptionHandler);
        event.dispose();
        return CompletableFuture.allOf(
            futures.toArray(new CompletableFuture[futures.size()])).thenCompose(x ->
            {
                List<List<TextEdit>> result = new ArrayList<>();
                for (CompletableFuture<List<TextEdit>> future : futures)
                {
                    List<TextEdit> edits = future.join();
                    if (edits != null)
                        result.add(edits);
                }
                return CompletableFuture.completedFuture(result);
            });
    }

    @Override
    public Disposable subscribe(Consumer<? super TextDocumentWillSaveEvent> consumer)
    {
        return delegate.subscribe(consumer);
    }

    @Override
    public void dispose()
    {
        delegate.dispose();
    }
}
