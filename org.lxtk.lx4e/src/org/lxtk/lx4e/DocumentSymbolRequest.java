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

import org.eclipse.lsp4j.DocumentSymbol;
import org.eclipse.lsp4j.DocumentSymbolParams;
import org.eclipse.lsp4j.SymbolInformation;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.lxtk.DocumentSymbolProvider;

/**
 * Requests information about symbols defined in the given text document.
 */
public class DocumentSymbolRequest
    extends LanguageFeatureRequest<DocumentSymbolProvider, DocumentSymbolParams,
        List<Either<SymbolInformation, DocumentSymbol>>>
{
    @Override
    protected CompletableFuture<List<Either<SymbolInformation, DocumentSymbol>>> send(
        DocumentSymbolProvider provider, DocumentSymbolParams params)
    {
        setTitle(MessageFormat.format(Messages.DocumentSymbolRequest_title, params));
        return provider.getDocumentSymbols(params);
    }
}
