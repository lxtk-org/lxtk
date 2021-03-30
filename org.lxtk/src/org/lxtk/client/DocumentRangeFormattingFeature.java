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
import org.eclipse.lsp4j.DocumentRangeFormattingOptions;
import org.eclipse.lsp4j.DocumentRangeFormattingParams;
import org.eclipse.lsp4j.DocumentRangeFormattingRegistrationOptions;
import org.eclipse.lsp4j.RangeFormattingCapabilities;
import org.eclipse.lsp4j.Registration;
import org.eclipse.lsp4j.ServerCapabilities;
import org.eclipse.lsp4j.TextDocumentClientCapabilities;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.lxtk.DocumentRangeFormattingProvider;
import org.lxtk.LanguageService;
import org.lxtk.ProgressService;
import org.lxtk.util.Disposable;

/**
 * Participates in a given {@link LanguageService} by implementing and
 * dynamically contributing {@link DocumentRangeFormattingProvider}s
 * according to LSP.
 * <p>
 * This implementation is thread-safe.
 * </p>
 */
public final class DocumentRangeFormattingFeature
    extends TextDocumentLanguageFeature<DocumentRangeFormattingRegistrationOptions>
{
    private static final String METHOD = "textDocument/rangeFormatting"; //$NON-NLS-1$
    private static final Set<String> METHODS = Collections.singleton(METHOD);

    /**
     * Constructor.
     *
     * @param languageService not <code>null</code>
     */
    public DocumentRangeFormattingFeature(LanguageService languageService)
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
        RangeFormattingCapabilities rangeFormatting =
            getLanguageService().getDocumentRangeFormattingCapabilities();
        rangeFormatting.setDynamicRegistration(true);
        capabilities.setRangeFormatting(rangeFormatting);
    }

    @Override
    void initialize(ServerCapabilities capabilities, List<DocumentFilter> documentSelector)
    {
        if (documentSelector == null)
            return;

        Either<Boolean, DocumentRangeFormattingOptions> capability =
            capabilities.getDocumentRangeFormattingProvider();
        if (capability == null
            || !(capability.isRight() || Boolean.TRUE.equals(capability.getLeft())))
            return;

        DocumentRangeFormattingRegistrationOptions registerOptions =
            new DocumentRangeFormattingRegistrationOptions();
        registerOptions.setDocumentSelector(documentSelector);

        DocumentRangeFormattingOptions options = capability.getRight();
        if (options != null)
        {
            registerOptions.setWorkDoneProgress(options.getWorkDoneProgress());
        }

        register(new Registration(UUID.randomUUID().toString(), METHOD, registerOptions));
    }

    @Override
    Class<DocumentRangeFormattingRegistrationOptions> getRegistrationOptionsClass()
    {
        return DocumentRangeFormattingRegistrationOptions.class;
    }

    @Override
    Disposable registerLanguageFeatureProvider(String method,
        DocumentRangeFormattingRegistrationOptions options)
    {
        return getLanguageService().getDocumentRangeFormattingProviders().add(
            new DocumentRangeFormattingProvider()
            {
                @Override
                public DocumentRangeFormattingRegistrationOptions getRegistrationOptions()
                {
                    return options;
                }

                @Override
                public ProgressService getProgressService()
                {
                    return getLanguageClient().getProgressService();
                }

                @Override
                public CompletableFuture<List<? extends TextEdit>> getRangeFormattingEdits(
                    DocumentRangeFormattingParams params)
                {
                    return getLanguageServer().getTextDocumentService().rangeFormatting(params);
                }
            });
    }
}
