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
package org.lxtk.client;

import java.net.URI;
import java.util.Collection;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiConsumer;

import org.eclipse.lsp4j.Diagnostic;
import org.lxtk.util.Disposable;

/**
 * TODO JavaDoc
 * <p>
 * This implementation is thread-safe. The underlying delegate need not be
 * thread-safe.
 * </p>
 */
public final class BufferingDiagnosticRequestor
    implements BiConsumer<URI, Collection<Diagnostic>>, Disposable
{
    private final BiConsumer<URI, Collection<Diagnostic>> delegate;
    private ExecutorService executor;
    private boolean disposed;

    /**
     * TODO JavaDoc
     *
     * @param delegate not <code>null</code>
     */
    public BufferingDiagnosticRequestor(
        BiConsumer<URI, Collection<Diagnostic>> delegate)
    {
        this.delegate = Objects.requireNonNull(delegate);
    }

    @Override
    public void dispose()
    {
        synchronized (this)
        {
            if (disposed)
                return;
            disposed = true;
        }

        if (executor != null)
            executor.shutdown();

        if (delegate instanceof Disposable)
            ((Disposable)delegate).dispose();
    }

    @Override
    public synchronized void accept(URI uri, Collection<Diagnostic> diagnostics)
    {
        if (disposed)
            return;

        if (executor == null)
            executor = Executors.newSingleThreadExecutor();

        executor.execute(() -> delegate.accept(uri, diagnostics));
    }
}
