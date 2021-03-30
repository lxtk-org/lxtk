/*******************************************************************************
 * Copyright (c) 2019, 2021 1C-Soft LLC.
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
import org.eclipse.lsp4j.Hover;
import org.eclipse.lsp4j.HoverCapabilities;
import org.eclipse.lsp4j.HoverOptions;
import org.eclipse.lsp4j.HoverParams;
import org.eclipse.lsp4j.HoverRegistrationOptions;
import org.eclipse.lsp4j.Registration;
import org.eclipse.lsp4j.ServerCapabilities;
import org.eclipse.lsp4j.TextDocumentClientCapabilities;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.lxtk.HoverProvider;
import org.lxtk.LanguageService;
import org.lxtk.ProgressService;
import org.lxtk.util.Disposable;

/**
 * Participates in a given {@link LanguageService} by implementing and
 * dynamically contributing {@link HoverProvider}s according to LSP.
 * <p>
 * This implementation is thread-safe.
 * </p>
 */
public final class HoverFeature
    extends TextDocumentLanguageFeature<HoverRegistrationOptions>
{
    private static final String METHOD = "textDocument/hover"; //$NON-NLS-1$
    private static final Set<String> METHODS = Collections.singleton(METHOD);

    /**
     * Constructor.
     *
     * @param languageService not <code>null</code>
     */
    public HoverFeature(LanguageService languageService)
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
        HoverCapabilities hover = getLanguageService().getHoverCapabilities();
        hover.setDynamicRegistration(true);
        capabilities.setHover(hover);
    }

    @Override
    void initialize(ServerCapabilities capabilities, List<DocumentFilter> documentSelector)
    {
        if (documentSelector == null)
            return;

        Either<Boolean, HoverOptions> capability = capabilities.getHoverProvider();
        if (capability == null
            || !(capability.isRight() || Boolean.TRUE.equals(capability.getLeft())))
            return;

        HoverRegistrationOptions registerOptions = new HoverRegistrationOptions();
        registerOptions.setDocumentSelector(documentSelector);

        HoverOptions options = capability.getRight();
        if (options != null)
        {
            registerOptions.setWorkDoneProgress(options.getWorkDoneProgress());
        }

        register(new Registration(UUID.randomUUID().toString(), METHOD, registerOptions));
    }

    @Override
    Class<HoverRegistrationOptions> getRegistrationOptionsClass()
    {
        return HoverRegistrationOptions.class;
    }

    @Override
    Disposable registerLanguageFeatureProvider(String method, HoverRegistrationOptions options)
    {
        return getLanguageService().getHoverProviders().add(new HoverProvider()
        {
            @Override
            public HoverRegistrationOptions getRegistrationOptions()
            {
                return options;
            }

            @Override
            public ProgressService getProgressService()
            {
                return getLanguageClient().getProgressService();
            }

            @Override
            public CompletableFuture<Hover> getHover(HoverParams params)
            {
                return getLanguageServer().getTextDocumentService().hover(params);
            }
        });
    }
}
