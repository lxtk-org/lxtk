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

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.eclipse.lsp4j.SymbolInformation;
import org.eclipse.lsp4j.WorkspaceSymbolParams;
import org.eclipse.lsp4j.WorkspaceSymbolRegistrationOptions;

/**
 * Provides information about project-wide symbols matching a query string.
 *
 * @see LanguageService
 */
public interface WorkspaceSymbolProvider
    extends LanguageFeatureProvider<WorkspaceSymbolRegistrationOptions>
{
    /**
     * Requests information about project-wide symbols matching the given query string.
     *
     * @param params not <code>null</code>
     * @return result future (never <code>null</code>)
     */
    CompletableFuture<List<? extends SymbolInformation>> getWorkspaceSymbols(
        WorkspaceSymbolParams params);

    /**
     * Returns the context object associated with this provider.
     *
     * @return the associated context object, or <code>null</code> if none
     */
    Object getContext();
}
