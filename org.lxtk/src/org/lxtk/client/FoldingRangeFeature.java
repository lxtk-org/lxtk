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
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.eclipse.lsp4j.DocumentFilter;
import org.eclipse.lsp4j.FoldingRange;
import org.eclipse.lsp4j.FoldingRangeCapabilities;
import org.eclipse.lsp4j.FoldingRangeProviderOptions;
import org.eclipse.lsp4j.FoldingRangeRequestParams;
import org.eclipse.lsp4j.Registration;
import org.eclipse.lsp4j.ServerCapabilities;
import org.eclipse.lsp4j.TextDocumentClientCapabilities;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.lxtk.FoldingRangeProvider;
import org.lxtk.LanguageService;
import org.lxtk.util.Disposable;

/**
 * Participates in a given {@link LanguageService} by implementing and
 * dynamically contributing {@link FoldingRangeProvider}s according to LSP.
 * <p>
 * This implementation is thread-safe.
 * </p>
 */
public class FoldingRangeFeature
    extends TextDocumentLanguageFeature<FoldingRangeProviderOptions>
{
    private static final String METHOD = "textDocument/foldingRange"; //$NON-NLS-1$
    private static final Set<String> METHODS = Collections.singleton(METHOD);

    /**
     * Constructor.
     *
     * @param languageService not <code>null</code>
     */
    public FoldingRangeFeature(LanguageService languageService)
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
        FoldingRangeCapabilities foldingRange = getLanguageService().getFoldingRangeCapabilities();
        foldingRange.setDynamicRegistration(true);
        capabilities.setFoldingRange(foldingRange);
    }

    @Override
    void initialize(ServerCapabilities capabilities, List<DocumentFilter> documentSelector)
    {
        Either<Boolean, FoldingRangeProviderOptions> capability =
            capabilities.getFoldingRangeProvider();
        if (capability == null
            || !(capability.isRight() || Boolean.TRUE.equals(capability.getLeft())))
            return;

        FoldingRangeProviderOptions registerOptions = new FoldingRangeProviderOptions();
        FoldingRangeProviderOptions options = capability.getRight();
        if (options == null)
        {
            registerOptions.setDocumentSelector(documentSelector);
            registerOptions.setId(UUID.randomUUID().toString());
        }
        else
        {
            registerOptions.setDocumentSelector(
                Optional.ofNullable(options.getDocumentSelector()).orElse(documentSelector));
            registerOptions.setId(
                Optional.ofNullable(options.getId()).orElse(UUID.randomUUID().toString()));
        }

        register(new Registration(registerOptions.getId(), METHOD, registerOptions));
    }

    @Override
    Class<FoldingRangeProviderOptions> getRegistrationOptionsClass()
    {
        return FoldingRangeProviderOptions.class;
    }

    @Override
    Disposable registerLanguageFeatureProvider(String method, FoldingRangeProviderOptions options)
    {
        return getLanguageService().getFoldingRangeProviders().add(new FoldingRangeProvider()
        {
            @Override
            public FoldingRangeProviderOptions getRegistrationOptions()
            {
                return options;
            }

            @Override
            public CompletableFuture<List<FoldingRange>> getFoldingRanges(
                FoldingRangeRequestParams params)
            {
                return getLanguageServer().getTextDocumentService().foldingRange(params);
            }
        });
    }
}
