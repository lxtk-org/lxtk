/*******************************************************************************
 * Copyright (c) 2019, 2023 1C-Soft LLC.
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
package org.lxtk.client;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.eclipse.lsp4j.DocumentFilter;
import org.eclipse.lsp4j.DocumentSymbol;
import org.eclipse.lsp4j.DocumentSymbolCapabilities;
import org.eclipse.lsp4j.DocumentSymbolOptions;
import org.eclipse.lsp4j.DocumentSymbolParams;
import org.eclipse.lsp4j.DocumentSymbolRegistrationOptions;
import org.eclipse.lsp4j.Registration;
import org.eclipse.lsp4j.ServerCapabilities;
import org.eclipse.lsp4j.SymbolInformation;
import org.eclipse.lsp4j.TextDocumentClientCapabilities;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.lxtk.DocumentSymbolProvider;
import org.lxtk.LanguageService;
import org.lxtk.ProgressService;
import org.lxtk.util.Disposable;

/**
 * Participates in a given {@link LanguageService} by implementing and
 * dynamically contributing {@link DocumentSymbolProvider}s according to LSP.
 * <p>
 * This implementation is thread-safe.
 * </p>
 */
public final class DocumentSymbolFeature
    extends TextDocumentLanguageFeature<DocumentSymbolRegistrationOptions>
{
    private static final String METHOD = "textDocument/documentSymbol"; //$NON-NLS-1$
    private static final Set<String> METHODS = Collections.singleton(METHOD);

    /**
     * Constructor.
     *
     * @param languageService not <code>null</code>
     */
    public DocumentSymbolFeature(LanguageService languageService)
    {
        super(languageService);
    }

    @Override
    public Set<String> getMethods()
    {
        return METHODS;
    }

    @Override
    void fillClientCapabilities(TextDocumentClientCapabilities capabilities)
    {
        DocumentSymbolCapabilities documentSymbol =
            getLanguageService().getDocumentSymbolCapabilities();
        documentSymbol.setDynamicRegistration(true);
        capabilities.setDocumentSymbol(documentSymbol);
    }

    @Override
    void initialize(ServerCapabilities capabilities, List<DocumentFilter> documentSelector)
    {
        if (documentSelector == null)
            return;

        Either<Boolean, DocumentSymbolOptions> capability =
            capabilities.getDocumentSymbolProvider();
        if (capability == null
            || !(capability.isRight() || Boolean.TRUE.equals(capability.getLeft())))
            return;

        DocumentSymbolRegistrationOptions registerOptions = new DocumentSymbolRegistrationOptions();
        registerOptions.setDocumentSelector(documentSelector);

        DocumentSymbolOptions options = capability.getRight();
        if (options != null)
        {
            registerOptions.setWorkDoneProgress(options.getWorkDoneProgress());
            registerOptions.setLabel(options.getLabel());
        }

        register(new Registration(UUID.randomUUID().toString(), METHOD, registerOptions));
    }

    @Override
    Class<DocumentSymbolRegistrationOptions> getRegistrationOptionsClass()
    {
        return DocumentSymbolRegistrationOptions.class;
    }

    @Override
    Disposable registerLanguageFeatureProvider(String method,
        DocumentSymbolRegistrationOptions options)
    {
        return getLanguageService().getDocumentSymbolProviders().add(new DocumentSymbolProvider()
        {
            @Override
            public DocumentSymbolRegistrationOptions getRegistrationOptions()
            {
                return options;
            }

            @Override
            public ProgressService getProgressService()
            {
                return getLanguageClient().getProgressService();
            }

            @Override
            public CompletableFuture<
                List<Either<SymbolInformation, DocumentSymbol>>> getDocumentSymbols(
                    DocumentSymbolParams params)
            {
                return getLanguageServer().getTextDocumentService().documentSymbol(params);
            }
        });
    }
}
