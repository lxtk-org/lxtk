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

import org.eclipse.lsp4j.DocumentSymbol;
import org.eclipse.lsp4j.DocumentSymbolParams;
import org.eclipse.lsp4j.SymbolInformation;
import org.eclipse.lsp4j.jsonrpc.messages.Either;

/**
 * Provides information about symbols defined in a given text document.
 *
 * @see LanguageService
 */
public interface DocumentSymbolProvider
    extends LanguageFeatureProvider
{
    /**
     * Requests information about symbols defined in the given text document.
     *
     * @param params not <code>null</code>
     * @return result future (never <code>null</code>)
     */
    CompletableFuture<List<Either<SymbolInformation, DocumentSymbol>>> getDocumentSymbols(
        DocumentSymbolParams params);
}
