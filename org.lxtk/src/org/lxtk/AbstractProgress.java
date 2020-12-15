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

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import org.eclipse.lsp4j.ProgressParams;
import org.eclipse.lsp4j.jsonrpc.messages.Either;

/**
 * Partial implementation of the {@link Progress} interface.
 */
public abstract class AbstractProgress
    implements Progress
{
    private final CompletableFuture<Void> future = new CompletableFuture<>();
    private final Either<String, Number> token;
    private volatile long lastUpdated;

    /**
     * Constructor.
     *
     * @param token not <code>null</code>
     */
    public AbstractProgress(Either<String, Number> token)
    {
        this.token = Objects.requireNonNull(token);
    }

    @Override
    public final CompletableFuture<Void> toCompletableFuture()
    {
        return future;
    }

    @Override
    public final Either<String, Number> getToken()
    {
        return token;
    }

    @Override
    public final long getLastUpdated()
    {
        return lastUpdated;
    }

    @Override
    public final void accept(ProgressParams params)
    {
        if (!token.equals(params.getToken()))
            return;

        lastUpdated = System.currentTimeMillis();

        doAccept(params);
    }

    /**
     * Handles the progress notification.
     *
     * @param params never <code>null</code>
     */
    protected abstract void doAccept(ProgressParams params);
}
