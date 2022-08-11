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
import java.util.concurrent.CompletableFuture;

import org.eclipse.lsp4j.PrepareRenameDefaultBehavior;
import org.eclipse.lsp4j.PrepareRenameParams;
import org.eclipse.lsp4j.PrepareRenameResult;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.jsonrpc.messages.Either3;
import org.lxtk.RenameProvider;

/**
 * Requests preparation for rename of the symbol denoted by the given text document position.
 */
public class PrepareRenameRequest
    extends LanguageFeatureRequest<RenameProvider, PrepareRenameParams,
        Either3<Range, PrepareRenameResult, PrepareRenameDefaultBehavior>>
{
    @Override
    protected CompletableFuture<
        Either3<Range, PrepareRenameResult, PrepareRenameDefaultBehavior>> send(
            RenameProvider provider, PrepareRenameParams params)
    {
        setTitle(MessageFormat.format(Messages.PrepareRenameRequest_title, params));
        return provider.prepareRename(params);
    }
}
