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
package org.lxtk.client;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.eclipse.lsp4j.DocumentFilter;
import org.eclipse.lsp4j.DocumentLink;
import org.eclipse.lsp4j.DocumentLinkCapabilities;
import org.eclipse.lsp4j.DocumentLinkOptions;
import org.eclipse.lsp4j.DocumentLinkParams;
import org.eclipse.lsp4j.DocumentLinkRegistrationOptions;
import org.eclipse.lsp4j.Registration;
import org.eclipse.lsp4j.ServerCapabilities;
import org.eclipse.lsp4j.TextDocumentClientCapabilities;
import org.lxtk.DocumentLinkProvider;
import org.lxtk.LanguageService;
import org.lxtk.ProgressService;
import org.lxtk.util.Disposable;

/**
 * Participates in a given {@link LanguageService} by implementing and
 * dynamically contributing {@link DocumentLinkProvider}s according to LSP.
 * <p>
 * This implementation is thread-safe.
 * </p>
 */
public final class DocumentLinkFeature
    extends TextDocumentLanguageFeature<DocumentLinkRegistrationOptions>
{
    private static final String METHOD = "textDocument/documentLink"; //$NON-NLS-1$
    private static final Set<String> METHODS = Collections.singleton(METHOD);

    /**
     * Constructor.
     *
     * @param languageService not <code>null</code>
     */
    public DocumentLinkFeature(LanguageService languageService)
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
        DocumentLinkCapabilities documentLink = getLanguageService().getDocumentLinkCapabilities();
        documentLink.setDynamicRegistration(true);
        capabilities.setDocumentLink(documentLink);
    }

    @Override
    void initialize(ServerCapabilities capabilities, List<DocumentFilter> documentSelector)
    {
        if (documentSelector == null)
            return;

        DocumentLinkOptions capability = capabilities.getDocumentLinkProvider();
        if (capability == null)
            return;

        DocumentLinkRegistrationOptions options = new DocumentLinkRegistrationOptions();
        options.setDocumentSelector(documentSelector);
        options.setWorkDoneProgress(capability.getWorkDoneProgress());
        options.setResolveProvider(capability.getResolveProvider());
        register(new Registration(UUID.randomUUID().toString(), METHOD, options));
    }

    @Override
    Class<DocumentLinkRegistrationOptions> getRegistrationOptionsClass()
    {
        return DocumentLinkRegistrationOptions.class;
    }

    @Override
    Disposable registerLanguageFeatureProvider(String method,
        DocumentLinkRegistrationOptions options)
    {
        return getLanguageService().getDocumentLinkProviders().add(new DocumentLinkProvider()
        {
            @Override
            public DocumentLinkRegistrationOptions getRegistrationOptions()
            {
                return options;
            }

            @Override
            public ProgressService getProgressService()
            {
                return getLanguageClient().getProgressService();
            }

            @Override
            public CompletableFuture<List<DocumentLink>> getDocumentLinks(DocumentLinkParams params)
            {
                return getLanguageServer().getTextDocumentService().documentLink(params);
            }

            @Override
            public CompletableFuture<DocumentLink> resolveDocumentLink(DocumentLink unresolved)
            {
                if (!Boolean.TRUE.equals(options.getResolveProvider()))
                    throw new UnsupportedOperationException();

                return getLanguageServer().getTextDocumentService().documentLinkResolve(unresolved);
            }
        });
    }
}
