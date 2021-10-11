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

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * A special event to support waiting for the results of asynchronous computations scheduled by
 * consumers of the event. For example, in case of a 'willSave' event for a text document,
 * event consumers could compute text edits to be applied to the document before it is saved.
 *
 * @param <E> the type of the wrapped event
 * @param <T> the result type of asynchronous computations scheduled in response to the event
 * @see WaitUntilEventEmitter
 */
public class WaitUntilEvent<E, T>
    implements Supplier<E>, Consumer<CompletableFuture<T>>
{
    private final E event;
    private final Consumer<? super CompletableFuture<T>> consumer;

    /**
     * Constructor.
     *
     * @param event not <code>null</code>
     * @param consumer not <code>null</code>
     */
    public WaitUntilEvent(E event, Consumer<? super CompletableFuture<T>> consumer)
    {
        this.event = Objects.requireNonNull(event);
        this.consumer = Objects.requireNonNull(consumer);
    }

    /**
     * Returns the wrapped event.
     *
     * @return the wrapped event (never <code>null</code>)
     */
    @Override
    public E get()
    {
        return event;
    }

    /**
     * Accepts the future of an asynchronous computation scheduled in response to the event.
     * <p>
     * <b>Note:</b> This method should only be called in the dynamic scope of the event notification.
     * </p>
     *
     * @param future not <code>null</code>
     */
    @Override
    public void accept(CompletableFuture<T> future)
    {
        consumer.accept(Objects.requireNonNull(future));
    }
}
