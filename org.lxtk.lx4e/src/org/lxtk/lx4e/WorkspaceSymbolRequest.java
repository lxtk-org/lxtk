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
package org.lxtk.lx4e;

import java.text.MessageFormat;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.eclipse.lsp4j.SymbolInformation;
import org.eclipse.lsp4j.WorkspaceSymbolParams;
import org.lxtk.WorkspaceSymbolProvider;

/**
 * Requests information about project-wide symbols matching the given query string.
 */
public class WorkspaceSymbolRequest
    extends LanguageFeatureRequest<WorkspaceSymbolProvider, WorkspaceSymbolParams,
        List<? extends SymbolInformation>>
{
    @Override
    protected CompletableFuture<List<? extends SymbolInformation>> send(
        WorkspaceSymbolProvider provider, WorkspaceSymbolParams params)
    {
        setTitle(MessageFormat.format(Messages.WorkspaceSymbolRequest_title, params));
        return provider.getWorkspaceSymbols(params);
    }
}
