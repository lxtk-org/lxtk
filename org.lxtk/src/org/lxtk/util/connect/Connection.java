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
package org.lxtk.util.connect;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;

import org.lxtk.util.Disposable2;

/**
 * TODO JavaDoc
 */
public interface Connection
    extends Disposable2
{
    /**
     * TODO JavaDoc
     *
     * @return <code>true</code> if the connection got closed,
     *  and <code>false</code> otherwise
     */
    boolean isClosed();

    /**
     * TODO JavaDoc
     *
     * @param executor not <code>null</code>
     * @return a stage that completes when the connection gets closed
     *  (never <code>null</code>)
     */
    default CompletionStage<?> monitor(Executor executor)
    {
        return CompletableFuture.anyOf(onDispose().toCompletableFuture(),
            CompletableFuture.runAsync(new ConnectionMonitor(this), executor));
    }
}
