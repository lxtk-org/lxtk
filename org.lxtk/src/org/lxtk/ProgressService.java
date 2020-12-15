/*******************************************************************************
 * Copyright (c) 2020 1C-Soft LLC.
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

import java.util.function.Consumer;

import org.eclipse.lsp4j.ProgressParams;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.lxtk.util.Disposable;

/**
 * Provides support for progress reporting.
 *
 * @see DefaultProgressService
 */
public interface ProgressService
    extends Consumer<ProgressParams>
{
    /**
     * Associates the given progress consumer with the given progress token.
     *
     * @param token not <code>null</code>
     * @param consumer not <code>null</code>
     * @return a disposable to dissociate the consumer (never null)
     */
    Disposable onProgress(Either<String, Number> token, Consumer<? super ProgressParams> consumer);

    /**
     * Attaches the given progress object to this service.
     * The attached progress is automatically detached when completed.
     *
     * @param progress not <code>null</code>
     */
    default void attachProgress(Progress progress)
    {
        Disposable subscription = onProgress(progress.getToken(), progress);
        progress.toCompletableFuture().whenComplete((result, thrown) -> subscription.dispose());
    }
}
