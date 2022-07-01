/*******************************************************************************
 * Copyright (c) 2022 1C-Soft LLC.
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
package org.lxtk.lx4e.requests;

import java.text.MessageFormat;
import java.util.concurrent.CompletableFuture;

import org.eclipse.lsp4j.WorkspaceSymbol;
import org.lxtk.WorkspaceSymbolProvider;

/**
 * Requests additional information for the given workspace symbol.
 */
public class WorkspaceSymbolResolveRequest
    extends LanguageFeatureRequest<WorkspaceSymbolProvider, WorkspaceSymbol, WorkspaceSymbol>
{
    @Override
    protected CompletableFuture<WorkspaceSymbol> send(WorkspaceSymbolProvider provider,
        WorkspaceSymbol workspaceSymbol)
    {
        setTitle(
            MessageFormat.format(Messages.WorkspaceSymbolResolveRequest_title, workspaceSymbol));
        return provider.resolveWorkspaceSymbol(workspaceSymbol);
    }
}
