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
package org.lxtk.util;

import java.util.function.Consumer;

/**
 * Represents an event stream that clients can subscribe to.
 *
 * @param <T> event type
 */
public interface EventStream<T>
{
    /**
     * Subscribes the given event consumer to this stream.
     *
     * @param consumer not <code>null</code>
     * @return a disposable to unsubscribe the consumer
     *  (never <code>null</code>)
     */
    Disposable subscribe(Consumer<? super T> consumer);
}
