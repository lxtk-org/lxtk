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

import java.util.concurrent.CompletionStage;

/**
 * Extends the {@link Disposable} interface with methods to determine
 * whether the object got disposed and to perform an action when the object
 * gets disposed.
 *
 * @see DisposableObject
 */
public interface Disposable2
    extends Disposable
{
    /**
     * Checks whether this object got disposed.
     *
     * @return <code>true</code> if the object got disposed,
     *  and <code>false</code> otherwise
     */
    default boolean isDisposed()
    {
        return onDispose().toCompletableFuture().isDone();
    }

    /**
     * Returns a stage that completes when this object gets disposed.
     *
     * @return a stage that completes when the object gets disposed
     *  (never <code>null</code>)
     */
    CompletionStage<?> onDispose();
}
