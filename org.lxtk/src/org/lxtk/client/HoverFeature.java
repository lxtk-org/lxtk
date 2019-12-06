/*******************************************************************************
 * Copyright (c) 2019 1C-Soft LLC.
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
import org.eclipse.lsp4j.Registration;
import org.eclipse.lsp4j.ServerCapabilities;
import org.eclipse.lsp4j.TextDocumentClientCapabilities;
import org.eclipse.lsp4j.TextDocumentPositionParams;
import org.eclipse.lsp4j.TextDocumentRegistrationOptions;
import org.lxtk.HoverProvider;
import org.lxtk.LanguageService;
import org.lxtk.util.Disposable;

/**
 * TODO JavaDoc
 */
public class HoverFeature
    extends LanguageFeature
{
    private static final String METHOD = "textDocument/hover"; //$NON-NLS-1$
    private static final Set<String> METHODS = Collections.singleton(METHOD);

    /**
     * TODO JavaDoc
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
    protected void fillClientCapabilities(
        TextDocumentClientCapabilities capabilities)
    {
        capabilities.setHover(getLanguageService().getHoverCapabilities());
    }

    @Override
    protected void initialize(ServerCapabilities capabilities,
        List<DocumentFilter> documentSelector)
    {
        if (documentSelector == null)
            return;

        Boolean capability = capabilities.getHoverProvider();
        if (!Boolean.TRUE.equals(capability))
            return;

        register(new Registration(UUID.randomUUID().toString(), METHOD,
            new TextDocumentRegistrationOptions(documentSelector)));
    }

    @Override
    protected Disposable registerLanguageFeatureProvider(String method,
        TextDocumentRegistrationOptions options)
    {
        return getLanguageService().getHoverProviders().add(new HoverProvider()
        {
            @Override
            public TextDocumentRegistrationOptions getRegistrationOptions()
            {
                return options;
            }

            @Override
            public CompletableFuture<Hover> getHover(
                TextDocumentPositionParams params)
            {
                return getLanguageServer().getTextDocumentService().hover(
                    params);
            }
        });
    }
}
