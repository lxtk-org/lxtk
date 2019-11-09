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

import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * TODO JavaDoc
 *
 * @param <E> element type
 */
public interface Registry<E>
    extends Iterable<E>
{
    /**
     * TODO JavaDoc
     *
     * @param e element to add (not <code>null</code>)
     * @return a disposable to remove the added element (never <code>null</code>)
     */
    Disposable add(E e);

    /**
     * TODO JavaDoc
     *
     * @return an event emitter firing when an element is added
     */
    EventStream<E> onDidAdd();

    /**
     * TODO JavaDoc
     *
     * @return an event emitter firing when an element is removed
     */
    EventStream<E> onDidRemove();

    /**
     * TODO JavaDoc
     *
     * @param <E> element type
     * @return a new instance of a default registry implementation
     *  (never <code>null</code>)
     */
    static <E> Registry<E> newInstance()
    {
        return new Registry<E>()
        {
            private final Set<E> elements = new CopyOnWriteArraySet<>();
            private final EventEmitter<E> onDidAdd = new EventEmitter<>();
            private final EventEmitter<E> onDidRemove = new EventEmitter<>();

            @Override
            public Iterator<E> iterator()
            {
                return new Iterator<E>() // "unmodifiable" iterator
                {
                    private final Iterator<E> it = elements.iterator();

                    @Override
                    public boolean hasNext()
                    {
                        return it.hasNext();
                    }

                    @Override
                    public E next()
                    {
                        return it.next();
                    }
                };
            }

            @Override
            public Disposable add(E e)
            {
                if (elements.add(e))
                    onDidAdd.fire(e);
                return () ->
                {
                    if (elements.remove(e))
                        onDidRemove.fire(e);
                };
            }

            @Override
            public EventStream<E> onDidAdd()
            {
                return onDidAdd;
            }

            @Override
            public EventStream<E> onDidRemove()
            {
                return onDidRemove;
            }
        };
    }
}
