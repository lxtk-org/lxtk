/*******************************************************************************
 * Copyright (c) 2020, 2021 1C-Soft LLC.
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
package org.lxtk;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import org.eclipse.lsp4j.ProgressParams;
import org.eclipse.lsp4j.jsonrpc.messages.Either;

/**
 * Represents a progress.
 */
public interface Progress
    extends Consumer<ProgressParams>
{
    /**
     * Returns the progress token.
     *
     * @return the progress token (never <code>null</code>)
     */
    Either<String, Integer> getToken();

    /**
     * Returns the progress future.
     *
     * @return the progress future (never <code>null</code>)
     */
    CompletableFuture<Void> toCompletableFuture();

    /**
     * Returns the time the progress was last updated.
     *
     * @return the time the progress was last updated, measured in milliseconds since the epoch
     *  (00:00:00 GMT, January 1, 1970), or <code>0L</code> if not available (e.g., the progress
     *  has not been updated).
     */
    long getLastUpdated();

    /**
     * Connects the progress with the given future.
     * <p>
     * The default implementation ensures that:
     * <ul>
     * <li>If not already completed, the progress completes when the given future is complete.
     * <li>If not already completed, the given future gets canceled when the progress is canceled.
     * </ul>
     *
     * @param future not <code>null</code>
     */
    default void connectWith(CompletableFuture<?> future)
    {
        CompletableFuture<Void> progressFuture = toCompletableFuture();
        future.whenComplete((result, thrown) -> progressFuture.complete(null));
        progressFuture.whenComplete((result, thrown) ->
        {
            if (progressFuture.isCancelled())
                future.cancel(true);
        });
    }
}
