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
package org.lxtk;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.lxtk.util.Disposable;

/**
 * TODO JavaDoc
 */
public interface CommandService
{
    /**
     * TODO JavaDoc
     * <p>
     * If a command with the given id is already registered,
     * an exception will be thrown.
     * </p>
     *
     * @param command a unique command id (not <code>null</code>)
     * @param handler a command handler (not <code>null</code>)
     * @return a disposable to remove the added command (never <code>null</code>)
     */
    Disposable addCommand(String command, CommandHandler handler);

    /**
     * TODO JavaDoc
     * <p>
     * If a command with the given id is not registered,
     * an exception will be thrown.
     * </p>
     *
     * @param command the command to execute (not <code>null</code>)
     * @param arguments may be <code>null</code>
     * @return result future (never <code>null</code>)
     */
    CompletableFuture<Object> executeCommand(String command,
        List<Object> arguments);

    /**
     * TODO JavaDoc
     *
     * @return all commands currently known to the service
     *  (never <code>null</code>, may be empty)
     */
    Set<String> getCommands();
}
