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

import java.util.concurrent.CompletableFuture;

import org.eclipse.lsp4j.PrepareRenameDefaultBehavior;
import org.eclipse.lsp4j.PrepareRenameParams;
import org.eclipse.lsp4j.PrepareRenameResult;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.RenameOptions;
import org.eclipse.lsp4j.RenameParams;
import org.eclipse.lsp4j.WorkspaceEdit;
import org.eclipse.lsp4j.jsonrpc.messages.Either3;

/**
 * Provides the {@link WorkspaceEdit} for renaming the symbol denoted by
 * a given text document position.
 *
 * @see LanguageService
 */
public interface RenameProvider
    extends LanguageFeatureProvider<RenameOptions>
{
    /**
     * Requests the workspace edit for the given {@link RenameParams}.
     *
     * @param params not <code>null</code>
     * @return result future (never <code>null</code>)
     */
    CompletableFuture<WorkspaceEdit> getRenameEdits(RenameParams params);

    /**
     * Requests preparation for rename of the symbol denoted by the given text document position.
     *
     * @param params not <code>null</code>
     * @return result future (never <code>null</code>)
     * @throws UnsupportedOperationException if no support for rename preparation
     *  is available
     * @see RenameOptions#getPrepareProvider()
     */
    CompletableFuture<
        Either3<Range, PrepareRenameResult, PrepareRenameDefaultBehavior>> prepareRename(
            PrepareRenameParams params);
}
