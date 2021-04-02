/*******************************************************************************
 * Copyright (c) 2019, 2021 1C-Soft LLC.
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

import java.util.Set;

import org.lxtk.util.Disposable;

/**
 * Provides support for command management. A command is a {@link CommandHandler}
 * with a unique identifier.
 *
 * @see DefaultCommandService
 */
public interface CommandService
{
    /**
     * Adds a new command.
     * <p>
     * If a command with the given identifier is already present,
     * a runtime exception is thrown.
     * </p>
     *
     * @param command a unique command identifier (not <code>null</code>)
     * @param handler a command handler (not <code>null</code>)
     * @return a disposable to remove the added command (never <code>null</code>)
     */
    Disposable addCommand(String command, CommandHandler handler);

    /**
     * Returns the handler currently associated with the given command.
     *
     * @param command may be <code>null</code>, in which case <code>null</code> is returned
     * @return the corresponding handler, or <code>null</code> if none
     */
    CommandHandler getCommandHandler(String command);

    /**
     * Returns all commands currently known to this service.
     *
     * @return all commands currently known to the service
     * (never <code>null</code>, may be empty). Clients
     * <b>must not</b> modify the returned set
     */
    Set<String> getCommands();
}
