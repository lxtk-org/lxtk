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
package org.lxtk;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import org.lxtk.util.Disposable;

/**
 * Default implementation of the {@link CommandService} interface.
 * <p>
 * This implementation is thread-safe.
 * </p>
 */
public class DefaultCommandService
    implements CommandService
{
    private final Map<String, CommandHandler> commands =
        new ConcurrentHashMap<>();

    @Override
    public Disposable addCommand(String command, CommandHandler handler)
    {
        if (commands.putIfAbsent(Objects.requireNonNull(command),
            Objects.requireNonNull(handler)) != null)
        {
            throw new IllegalArgumentException("Command already exists: " //$NON-NLS-1$
                + command);
        }
        return () -> commands.remove(command, handler);
    }

    @Override
    public CompletableFuture<Object> executeCommand(String command,
        List<Object> arguments)
    {
        CommandHandler handler = commands.get(command);
        if (handler == null)
            return null;
        return handler.execute(arguments);
    }

    @Override
    public Set<String> getCommands()
    {
        return Collections.unmodifiableSet(commands.keySet());
    }
}
