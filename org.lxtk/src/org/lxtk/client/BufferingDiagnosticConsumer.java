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
package org.lxtk.client;

import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.eclipse.lsp4j.PublishDiagnosticsParams;
import org.lxtk.util.Disposable;

/**
 * A consumer of LSP diagnostics that invokes a given delegate asynchronously,
 * in a dedicated working thread.
 * <p>
 * This implementation is thread-safe. The delegate need not be thread-safe.
 * </p>
 */
public final class BufferingDiagnosticConsumer
    implements Consumer<PublishDiagnosticsParams>, Disposable
{
    private final Consumer<PublishDiagnosticsParams> delegate;
    private ExecutorService executor;
    private boolean disposed;

    /**
     * Constructor.
     *
     * @param delegate not <code>null</code>
     */
    public BufferingDiagnosticConsumer(Consumer<PublishDiagnosticsParams> delegate)
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
        {
            if (delegate instanceof Disposable)
                executor.execute(() -> ((Disposable)delegate).dispose());

            executor.shutdown();
            try
            {
                executor.awaitTermination(5, TimeUnit.SECONDS);
            }
            catch (InterruptedException e)
            {
            }
        }
        else if (delegate instanceof Disposable)
            ((Disposable)delegate).dispose();
    }

    @Override
    public synchronized void accept(PublishDiagnosticsParams params)
    {
        if (disposed)
            return;

        if (executor == null)
            executor = Executors.newSingleThreadExecutor();

        executor.execute(() ->
        {
            if (!executor.isShutdown())
                delegate.accept(params);
        });
    }
}
