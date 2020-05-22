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
package org.lxtk.lx4e.requests;

import java.text.MessageFormat;
import java.util.List;
import java.util.concurrent.Future;

import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionList;
import org.eclipse.lsp4j.CompletionParams;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.lxtk.CompletionProvider;

/**
 * A request for computing completion items.
 */
public class CompletionRequest
    extends LanguageFeatureRequest<CompletionProvider, CompletionParams,
        Either<List<CompletionItem>, CompletionList>>
{
    @Override
    protected Future<Either<List<CompletionItem>, CompletionList>> send(
        CompletionProvider provider, CompletionParams params)
    {
        setTitle(
            MessageFormat.format(Messages.CompletionRequest_title, params));
        return provider.getCompletionItems(params);
    }
}
