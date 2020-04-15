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

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Represents a handler for a command.
 *
 * @see CommandService
 */
public interface CommandHandler
{
    /**
     * Executes this handler using the given arguments.
     *
     * @param arguments may be <code>null</code> or empty
     * @return result future (never <code>null</code>)
     */
    CompletableFuture<Object> execute(List<Object> arguments);
}
