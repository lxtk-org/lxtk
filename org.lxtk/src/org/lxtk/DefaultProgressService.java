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

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import org.eclipse.lsp4j.ProgressParams;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.lxtk.util.Disposable;

/**
 * Default implementation of the {@link ProgressService} interface.
 * <p>
 * This implementation is thread-safe.
 * </p>
 */
public class DefaultProgressService
    implements ProgressService
{
    private final Map<Either<String, Number>, Consumer<? super ProgressParams>> progressConsumers =
        new ConcurrentHashMap<>();

    @Override
    public void accept(ProgressParams params)
    {
        Consumer<? super ProgressParams> consumer = progressConsumers.get(params.getToken());
        if (consumer != null)
            consumer.accept(params);
    }

    @Override
    public Disposable onProgress(Either<String, Number> token,
        Consumer<? super ProgressParams> consumer)
    {
        Consumer<? super ProgressParams> existing = progressConsumers.putIfAbsent(
            Objects.requireNonNull(token), Objects.requireNonNull(consumer));
        if (existing != null && !existing.equals(consumer))
        {
            throw new IllegalArgumentException(
                "The service already manages another progress consumer for the token " //$NON-NLS-1$
                    + token);
        }
        return () -> progressConsumers.remove(token, consumer);
    }
}
