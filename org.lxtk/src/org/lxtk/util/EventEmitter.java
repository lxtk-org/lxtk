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
package org.lxtk.util;

import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Consumer;

/**
 * Default implementation of the {@link EventStream} interface.
 * <p>
 * This implementation is thread-safe.
 * </p>
 *
 * @param <E> event type
 */
public class EventEmitter<E>
    implements EventStream<E>, Disposable
{
    private final Set<Consumer<? super E>> consumers = new CopyOnWriteArraySet<>();

    @Override
    public Disposable subscribe(Consumer<? super E> consumer)
    {
        consumers.add(Objects.requireNonNull(consumer));
        return () -> consumers.remove(consumer);
    }

    /**
     * Notify all subscribers about the given event; the given exception handler
     * is used to handle any exception thrown by an event consumer.
     *
     * @param event may be <code>null</code>
     * @param exceptionHandler may be <code>null</code>, in which case
     *  any exception thrown by an event consumer is suppressed
     */
    public void emit(E event, Consumer<Throwable> exceptionHandler)
    {
        for (Consumer<? super E> consumer : consumers)
        {
            try
            {
                consumer.accept(event);
            }
            catch (Throwable t)
            {
                if (exceptionHandler != null)
                    exceptionHandler.accept(t);
                else
                    t.printStackTrace();
            }
        }
    }

    /**
     * Notify all subscribers about the given event; the given exception handler
     * is used to handle any exception thrown by an event consumer.
     *
     * @param event not <code>null</code>
     * @param exceptionHandler may be <code>null</code>, in which case
     *  any exception thrown by an event consumer is suppressed
     * @deprecated Replaced with {@link #emit(Object, Consumer)}
     */
    public void fire(E event, Consumer<Throwable> exceptionHandler)
    {
        emit(event, exceptionHandler);
    }

    @Override
    public void dispose()
    {
        consumers.clear();
    }
}
