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
package org.lxtk.lx4e;

import java.text.MessageFormat;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionList;
import org.eclipse.lsp4j.CompletionParams;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.lxtk.CompletionProvider;

/**
 * Requests completion items for the given {@link CompletionParams}.
 */
public class CompletionRequest
    extends LanguageFeatureRequestWithWorkDoneAndPartialResultProgress<CompletionProvider,
        CompletionParams, Either<List<CompletionItem>, CompletionList>>
{
    @Override
    protected CompletableFuture<Either<List<CompletionItem>, CompletionList>> send(
        CompletionProvider provider, CompletionParams params)
    {
        setTitle(MessageFormat.format(Messages.CompletionRequest_title, params));
        return provider.getCompletionItems(params);
    }
}
