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
package org.lxtk.util;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Default implementation of an {@link EventStream} of {@link WaitUntilEvent}s.
 * <p>
 * This implementation is thread-safe.
 * </p>
 *
 * @param <E> the type of the event wrapped by an emitted {@link WaitUntilEvent}
 * @param <T> the result type of asynchronous computations scheduled in response to emitted events
 */
public class WaitUntilEventEmitter<E, T>
    implements EventStream<WaitUntilEvent<E, T>>, Disposable
{
    private static final CompletableFuture<?>[] NO_FUTURES = new CompletableFuture[0];

    private final EventEmitter<WaitUntilEvent<E, T>> delegate = new EventEmitter<>();

    @Override
    public Disposable subscribe(Consumer<? super WaitUntilEvent<E, T>> consumer)
    {
        return delegate.subscribe(consumer);
    }

    /**
     * Notify all subscribers about a {@link WaitUntilEvent} that wraps the given event;
     * the given exception handler is used to handle any exception thrown by an event consumer.
     *
     * @param event not <code>null</code>
     * @param exceptionHandler may be <code>null</code>, in which case
     *  any exception thrown by an event consumer is suppressed
     * @return a future that is completed when all of the futures passed by event consumers to
     *  the event's {@link WaitUntilEvent#accept(CompletableFuture) accept} method complete
     *  (never <code>null</code>)
     */
    public CompletableFuture<List<T>> fire(E event, Consumer<Throwable> exceptionHandler)
    {
        List<CompletableFuture<T>> futureList = new ArrayList<>();
        delegate.fire(new WaitUntilEvent<>(event, futureList::add), exceptionHandler);
        @SuppressWarnings("unchecked")
        CompletableFuture<T>[] futures = (CompletableFuture<T>[])futureList.toArray(NO_FUTURES);
        return CompletableFuture.allOf(futures).thenCompose(x ->
        {
            List<T> result = new ArrayList<>();
            for (CompletableFuture<T> future : futures)
            {
                T value = future.join();
                if (value != null)
                    result.add(value);
            }
            return CompletableFuture.completedFuture(result);
        });
    }

    @Override
    public void dispose()
    {
        delegate.dispose();
    }
}
