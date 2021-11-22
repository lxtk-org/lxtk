/*******************************************************************************
 * Copyright (c) 2021 1C-Soft LLC.
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

import org.eclipse.lsp4j.SemanticTokens;
import org.eclipse.lsp4j.SemanticTokensDelta;
import org.eclipse.lsp4j.SemanticTokensDeltaParams;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.lxtk.DocumentSemanticTokensProvider;

/**
 * Requests information about semantic tokens delta for the given text document.
 */
public class DocumentSemanticTokensDeltaRequest
    extends
    LanguageFeatureRequestWithWorkDoneAndPartialResultProgress<DocumentSemanticTokensProvider,
        SemanticTokensDeltaParams, Either<SemanticTokens, SemanticTokensDelta>>
{
    @Override
    protected CompletableFuture<Either<SemanticTokens, SemanticTokensDelta>> send(
        DocumentSemanticTokensProvider provider, SemanticTokensDeltaParams params)
    {
        setTitle(MessageFormat.format(Messages.DocumentSemanticTokensDeltaRequest_title, params));
        return provider.getDocumentSemanticTokensDelta(params);
    }
}
