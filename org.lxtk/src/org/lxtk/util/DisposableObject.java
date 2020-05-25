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

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * Default implementation of the {@link Disposable2} interface that clients
 * can extend.
 * <p>
 * This implementation is thread-safe.
 * </p>
 */
public class DisposableObject
    implements Disposable2
{
    private final CompletableFuture<?> disposeFuture = new CompletableFuture<>();

    @Override
    public void dispose()
    {
        disposeFuture.complete(null);
    }

    @Override
    public CompletionStage<?> onDispose()
    {
        return disposeFuture;
    }
}
