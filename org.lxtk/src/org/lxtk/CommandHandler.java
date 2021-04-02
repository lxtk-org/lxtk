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

import java.util.concurrent.CompletableFuture;

import org.eclipse.lsp4j.ExecuteCommandParams;
import org.eclipse.lsp4j.ExecuteCommandRegistrationOptions;

/**
 * Represents a handler for a command.
 *
 * @see CommandService
 */
public interface CommandHandler
{
    /**
     * Executes this handler using the given parameters.
     *
     * @param params not <code>null</code>
     * @return result future (never <code>null</code>)
     */
    CompletableFuture<Object> execute(ExecuteCommandParams params);

    /**
     * Returns registration options for this handler.
     *
     * @return registration options. Clients <b>must not</b> modify the returned object
     */
    ExecuteCommandRegistrationOptions getRegistrationOptions();

    /**
     * Returns the progress service for this handler.
     *
     * @return the progress service, or <code>null</code> if none
     */
    default ProgressService getProgressService()
    {
        return null;
    }
}
