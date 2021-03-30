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
package org.lxtk.client;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.eclipse.lsp4j.DocumentFilter;
import org.eclipse.lsp4j.DocumentHighlight;
import org.eclipse.lsp4j.DocumentHighlightCapabilities;
import org.eclipse.lsp4j.DocumentHighlightOptions;
import org.eclipse.lsp4j.DocumentHighlightParams;
import org.eclipse.lsp4j.DocumentHighlightRegistrationOptions;
import org.eclipse.lsp4j.Registration;
import org.eclipse.lsp4j.ServerCapabilities;
import org.eclipse.lsp4j.TextDocumentClientCapabilities;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.lxtk.DocumentHighlightProvider;
import org.lxtk.LanguageService;
import org.lxtk.ProgressService;
import org.lxtk.util.Disposable;

/**
 * Participates in a given {@link LanguageService} by implementing and
 * dynamically contributing {@link DocumentHighlightProvider}s according to LSP.
 * <p>
 * This implementation is thread-safe.
 * </p>
 */
public final class DocumentHighlightFeature
    extends TextDocumentLanguageFeature<DocumentHighlightRegistrationOptions>
{
    private static final String METHOD = "textDocument/documentHighlight"; //$NON-NLS-1$
    private static final Set<String> METHODS = Collections.singleton(METHOD);

    /**
     * Constructor.
     *
     * @param languageService not <code>null</code>
     */
    public DocumentHighlightFeature(LanguageService languageService)
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
        DocumentHighlightCapabilities documentHighlight =
            getLanguageService().getDocumentHighlightCapabilities();
        documentHighlight.setDynamicRegistration(true);
        capabilities.setDocumentHighlight(documentHighlight);
    }

    @Override
    void initialize(ServerCapabilities capabilities, List<DocumentFilter> documentSelector)
    {
        if (documentSelector == null)
            return;

        Either<Boolean, DocumentHighlightOptions> capability =
            capabilities.getDocumentHighlightProvider();
        if (capability == null
            || !(capability.isRight() || Boolean.TRUE.equals(capability.getLeft())))
            return;

        DocumentHighlightRegistrationOptions registerOptions =
            new DocumentHighlightRegistrationOptions();
        registerOptions.setDocumentSelector(documentSelector);

        DocumentHighlightOptions options = capability.getRight();
        if (options != null)
        {
            registerOptions.setWorkDoneProgress(options.getWorkDoneProgress());
        }

        register(new Registration(UUID.randomUUID().toString(), METHOD, registerOptions));
    }

    @Override
    Class<DocumentHighlightRegistrationOptions> getRegistrationOptionsClass()
    {
        return DocumentHighlightRegistrationOptions.class;
    }

    @Override
    Disposable registerLanguageFeatureProvider(String method,
        DocumentHighlightRegistrationOptions options)
    {
        return getLanguageService().getDocumentHighlightProviders().add(
            new DocumentHighlightProvider()
            {
                @Override
                public DocumentHighlightRegistrationOptions getRegistrationOptions()
                {
                    return options;
                }

                @Override
                public ProgressService getProgressService()
                {
                    return getLanguageClient().getProgressService();
                }

                @Override
                public CompletableFuture<List<? extends DocumentHighlight>> getDocumentHighlights(
                    DocumentHighlightParams params)
                {
                    return getLanguageServer().getTextDocumentService().documentHighlight(params);
                }
            });
    }
}
