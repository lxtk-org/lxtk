/*******************************************************************************
 * Copyright (c) 2020, 2022 1C-Soft LLC.
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

import org.eclipse.lsp4j.WorkspaceSymbol;
import org.eclipse.lsp4j.WorkspaceSymbolParams;
import org.eclipse.lsp4j.WorkspaceSymbolRegistrationOptions;
import org.eclipse.lsp4j.jsonrpc.messages.Either;

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
    @SuppressWarnings("deprecation")
    CompletableFuture<Either<List<? extends org.eclipse.lsp4j.SymbolInformation>,
        List<? extends WorkspaceSymbol>>> getWorkspaceSymbols(WorkspaceSymbolParams params);

    /**
     * Requests additional information for the given workspace symbol.
     *
     * @param workspaceSymbol not <code>null</code>
     * @return result future (never <code>null</code>)
     * @throws UnsupportedOperationException if no support for resolving
     *  additional information for a workspace symbol is available
     * @see WorkspaceSymbolRegistrationOptions#getResolveProvider()
     */
    CompletableFuture<WorkspaceSymbol> resolveWorkspaceSymbol(WorkspaceSymbol workspaceSymbol);

    /**
     * Returns the context object associated with this provider.
     *
     * @return the associated context object, or <code>null</code> if none
     */
    Object getContext();
}
