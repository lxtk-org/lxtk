/*******************************************************************************
 * Copyright (c) 2022 1C-Soft LLC.
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
package org.lxtk.util;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.function.Consumer;

/**
 * An event emitter that uses an {@link Executor} for emitting events.
 * By default, a shared single-daemon-thread executor is used.
 * <p>
 * This implementation is thread-safe.
 * </p>
 *
 * @param <E> event type
 */
public class AsyncEventEmitter<E>
    extends EventEmitter<E>
{
    private static final Executor DEFAULT_EXECUTOR =
        Executors.newSingleThreadExecutor(new ThreadFactory()
        {
            @Override
            public Thread newThread(Runnable r)
            {
                Thread t = new Thread(r);
                t.setDaemon(true);
                t.setName(AsyncEventEmitter.class.getName());
                return t;
            }
        });

    @Override
    public void emit(E event, Consumer<Throwable> exceptionHandler)
    {
        getExecutor().execute(() -> super.emit(event, exceptionHandler));
    }

    /**
     * Returns the executor for emitting an event.
     *
     * @return an {@link Executor} (not <code>null</code>)
     */
    protected Executor getExecutor()
    {
        return DEFAULT_EXECUTOR;
    }
}
