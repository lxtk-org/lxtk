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
package org.lxtk.util;

import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Consumer;

/**
 * TODO JavaDoc
 *
 * @param <T> event type
 */
public class EventEmitter<T>
    implements EventStream<T>, Disposable
{
    private final Set<Consumer<T>> consumers = new CopyOnWriteArraySet<>();

    @Override
    public Disposable subscribe(Consumer<T> consumer)
    {
        consumers.add(consumer);
        return () -> consumers.remove(consumer);
    }

    /**
     * TODO JavaDoc
     *
     * @param event not <code>null</code>
     */
    public void fire(T event)
    {
        Objects.requireNonNull(event);
        for (Consumer<T> consumer : consumers)
        {
            try
            {
                consumer.accept(event);
            }
            catch (Exception e)
            {
                // ignore
            }
        }
    }

    @Override
    public void dispose()
    {
        consumers.clear();
    }
}
