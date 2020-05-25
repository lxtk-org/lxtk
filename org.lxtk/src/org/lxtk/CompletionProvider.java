/*******************************************************************************
 * Copyright (c) 2019, 2020 1C-Soft LLC.
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

import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionList;
import org.eclipse.lsp4j.CompletionParams;
import org.eclipse.lsp4j.CompletionRegistrationOptions;
import org.eclipse.lsp4j.jsonrpc.messages.Either;

/**
 * Provides {@link CompletionItem}s for a given text document position.
 *
 * @see LanguageService
 */
public interface CompletionProvider
    extends LanguageFeatureProvider
{
    @Override
    CompletionRegistrationOptions getRegistrationOptions();

    /**
     * Computes completion items for the given {@link CompletionParams}.
     *
     * @param params not <code>null</code>
     * @return result future (never <code>null</code>)
     */
    CompletableFuture<Either<List<CompletionItem>, CompletionList>> getCompletionItems(
        CompletionParams params);

    /**
     * Resolves additional information for the given completion item.
     *
     * @param item not <code>null</code>
     * @return result future (never <code>null</code>)
     * @throws UnsupportedOperationException if no support for resolving
     *  additional information for a completion item is available
     * @see CompletionRegistrationOptions#getResolveProvider()
     */
    CompletableFuture<CompletionItem> resolveCompletionItem(CompletionItem item);
}
