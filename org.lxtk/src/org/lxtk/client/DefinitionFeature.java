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

import org.eclipse.lsp4j.DefinitionCapabilities;
import org.eclipse.lsp4j.DefinitionOptions;
import org.eclipse.lsp4j.DefinitionParams;
import org.eclipse.lsp4j.DefinitionRegistrationOptions;
import org.eclipse.lsp4j.DocumentFilter;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.LocationLink;
import org.eclipse.lsp4j.Registration;
import org.eclipse.lsp4j.ServerCapabilities;
import org.eclipse.lsp4j.TextDocumentClientCapabilities;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.lxtk.DefinitionProvider;
import org.lxtk.LanguageService;
import org.lxtk.ProgressService;
import org.lxtk.util.Disposable;

/**
 * Participates in a given {@link LanguageService} by implementing and
 * dynamically contributing {@link DefinitionProvider}s according to LSP.
 * <p>
 * This implementation is thread-safe.
 * </p>
 */
public final class DefinitionFeature
    extends TextDocumentLanguageFeature<DefinitionRegistrationOptions>
{
    private static final String METHOD = "textDocument/definition"; //$NON-NLS-1$
    private static final Set<String> METHODS = Collections.singleton(METHOD);

    /**
     * Constructor.
     *
     * @param languageService not <code>null</code>
     */
    public DefinitionFeature(LanguageService languageService)
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
        DefinitionCapabilities definition = getLanguageService().getDefinitionCapabilities();
        definition.setDynamicRegistration(true);
        capabilities.setDefinition(definition);
    }

    @Override
    void initialize(ServerCapabilities capabilities, List<DocumentFilter> documentSelector)
    {
        if (documentSelector == null)
            return;

        Either<Boolean, DefinitionOptions> capability = capabilities.getDefinitionProvider();
        if (capability == null
            || !(capability.isRight() || Boolean.TRUE.equals(capability.getLeft())))
            return;

        DefinitionRegistrationOptions registerOptions = new DefinitionRegistrationOptions();
        registerOptions.setDocumentSelector(documentSelector);

        DefinitionOptions options = capability.getRight();
        if (options != null)
        {
            registerOptions.setWorkDoneProgress(options.getWorkDoneProgress());
        }

        register(new Registration(UUID.randomUUID().toString(), METHOD, registerOptions));
    }

    @Override
    Class<DefinitionRegistrationOptions> getRegistrationOptionsClass()
    {
        return DefinitionRegistrationOptions.class;
    }

    @Override
    Disposable registerLanguageFeatureProvider(String method, DefinitionRegistrationOptions options)
    {
        return getLanguageService().getDefinitionProviders().add(new DefinitionProvider()
        {
            @Override
            public DefinitionRegistrationOptions getRegistrationOptions()
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
                Either<List<? extends Location>, List<? extends LocationLink>>> getDefinition(
                    DefinitionParams params)
            {
                return getLanguageServer().getTextDocumentService().definition(params);
            }
        });
    }
}
