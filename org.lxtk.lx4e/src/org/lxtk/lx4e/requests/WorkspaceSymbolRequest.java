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
package org.lxtk.lx4e.requests;

import java.text.MessageFormat;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.eclipse.lsp4j.SymbolInformation;
import org.eclipse.lsp4j.WorkspaceSymbol;
import org.eclipse.lsp4j.WorkspaceSymbolParams;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.lxtk.WorkspaceSymbolProvider;

/**
 * Requests information about project-wide symbols matching the given query string.
 */
@SuppressWarnings("deprecation")
public class WorkspaceSymbolRequest
    extends
    LanguageFeatureRequestWithWorkDoneAndPartialResultProgress<WorkspaceSymbolProvider,
        WorkspaceSymbolParams,
        Either<List<? extends SymbolInformation>, List<? extends WorkspaceSymbol>>>
{
    @Override
    protected CompletableFuture<
        Either<List<? extends SymbolInformation>, List<? extends WorkspaceSymbol>>> send(
            WorkspaceSymbolProvider provider, WorkspaceSymbolParams params)
    {
        setTitle(MessageFormat.format(Messages.WorkspaceSymbolRequest_title, params));
        return provider.getWorkspaceSymbols(params);
    }
}
