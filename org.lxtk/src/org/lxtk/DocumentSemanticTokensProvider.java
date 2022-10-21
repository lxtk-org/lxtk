/*******************************************************************************
 * Copyright (c) 2021, 2022 1C-Soft LLC.
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

import org.eclipse.lsp4j.DocumentFilter;
import org.eclipse.lsp4j.SemanticTokens;
import org.eclipse.lsp4j.SemanticTokensDelta;
import org.eclipse.lsp4j.SemanticTokensDeltaParams;
import org.eclipse.lsp4j.SemanticTokensParams;
import org.eclipse.lsp4j.SemanticTokensRangeParams;
import org.eclipse.lsp4j.SemanticTokensServerFull;
import org.eclipse.lsp4j.SemanticTokensWithRegistrationOptions;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.lxtk.util.EventStream;

/**
 * Provides {@link SemanticTokens} for a given text document.
 *
 * @see LanguageService
 */
public interface DocumentSemanticTokensProvider
    extends LanguageFeatureProvider<SemanticTokensWithRegistrationOptions>
{
    @Override
    default List<DocumentFilter> getDocumentSelector()
    {
        return getRegistrationOptions().getDocumentSelector();
    }

    /**
     * Requests semantic tokens for the given text document.
     *
     * @param params not <code>null</code>
     * @return result future (never <code>null</code>)
     * @throws UnsupportedOperationException if no support for providing semantic tokens
     *  for a full document is available
     * @see SemanticTokensWithRegistrationOptions#getFull()
     */
    CompletableFuture<SemanticTokens> getDocumentSemanticTokens(SemanticTokensParams params);

    /**
     * Requests semantic tokens delta for the given text document.
     *
     * @param params not <code>null</code>
     * @return result future (never <code>null</code>)
     * @throws UnsupportedOperationException if no support for providing semantic tokens delta
     *  is available
     * @see SemanticTokensWithRegistrationOptions#getFull()
     * @see SemanticTokensServerFull#getDelta()
     */
    CompletableFuture<Either<SemanticTokens, SemanticTokensDelta>> getDocumentSemanticTokensDelta(
        SemanticTokensDeltaParams params);

    /**
     * Requests semantic tokens for the given text document range.
     *
     * @param params not <code>null</code>
     * @return result future (never <code>null</code>)
     * @throws UnsupportedOperationException if no support for providing semantic tokens
     *  for a document range is available
     * @see SemanticTokensWithRegistrationOptions#getRange()
     */
    CompletableFuture<SemanticTokens> getDocumentRangeSemanticTokens(
        SemanticTokensRangeParams params);

    /**
     * Returns a stream of events that are emitted when semantic tokens need to be refreshed.
     *
     * @return a stream of events that are emitted when semantic tokens need to be refreshed
     *  (never <code>null</code>)
     */
    EventStream<Void> onRefreshSemanticTokens();
}
