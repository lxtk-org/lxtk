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
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.eclipse.lsp4j.DocumentFilter;
import org.eclipse.lsp4j.LinkedEditingRangeCapabilities;
import org.eclipse.lsp4j.LinkedEditingRangeParams;
import org.eclipse.lsp4j.LinkedEditingRangeRegistrationOptions;
import org.eclipse.lsp4j.LinkedEditingRanges;
import org.eclipse.lsp4j.Registration;
import org.eclipse.lsp4j.ServerCapabilities;
import org.eclipse.lsp4j.TextDocumentClientCapabilities;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.lxtk.LanguageService;
import org.lxtk.LinkedEditingRangeProvider;
import org.lxtk.ProgressService;
import org.lxtk.util.Disposable;

/**
 * Participates in a given {@link LanguageService} by implementing and
 * dynamically contributing {@link LinkedEditingRangeProvider}s according to LSP.
 * <p>
 * This implementation is thread-safe.
 * </p>
 */
public final class LinkedEditingRangeFeature
    extends TextDocumentLanguageFeature<LinkedEditingRangeRegistrationOptions>
{
    private static final String METHOD = "textDocument/linkedEditingRange"; //$NON-NLS-1$
    private static final Set<String> METHODS = Collections.singleton(METHOD);

    /**
     * Constructor.
     *
     * @param languageService not <code>null</code>
     */
    public LinkedEditingRangeFeature(LanguageService languageService)
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
        LinkedEditingRangeCapabilities linkedEditingRange =
            getLanguageService().getLinkedEditingRangeCapabilities();
        linkedEditingRange.setDynamicRegistration(true);
        capabilities.setLinkedEditingRange(linkedEditingRange);
    }

    @Override
    void initialize(ServerCapabilities capabilities, List<DocumentFilter> documentSelector)
    {
        Either<Boolean, LinkedEditingRangeRegistrationOptions> capability =
            capabilities.getLinkedEditingRangeProvider();
        if (capability == null
            || !(capability.isRight() || Boolean.TRUE.equals(capability.getLeft())))
            return;

        LinkedEditingRangeRegistrationOptions registerOptions =
            new LinkedEditingRangeRegistrationOptions();
        LinkedEditingRangeRegistrationOptions options = capability.getRight();
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
            registerOptions.setWorkDoneProgress(options.getWorkDoneProgress());
        }

        register(new Registration(registerOptions.getId(), METHOD, registerOptions));
    }

    @Override
    Class<LinkedEditingRangeRegistrationOptions> getRegistrationOptionsClass()
    {
        return LinkedEditingRangeRegistrationOptions.class;
    }

    @Override
    Disposable registerLanguageFeatureProvider(String method,
        LinkedEditingRangeRegistrationOptions options)
    {
        return getLanguageService().getLinkedEditingRangeProviders().add(
            new LinkedEditingRangeProvider()
            {
                @Override
                public LinkedEditingRangeRegistrationOptions getRegistrationOptions()
                {
                    return options;
                }

                @Override
                public ProgressService getProgressService()
                {
                    return getLanguageClient().getProgressService();
                }

                @Override
                public CompletableFuture<LinkedEditingRanges> getLinkedEditingRanges(
                    LinkedEditingRangeParams params)
                {
                    return getLanguageServer().getTextDocumentService().linkedEditingRange(params);
                }
            });
    }
}
