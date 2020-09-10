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
package org.lxtk.client;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.eclipse.lsp4j.DocumentFilter;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.LocationLink;
import org.eclipse.lsp4j.Registration;
import org.eclipse.lsp4j.ServerCapabilities;
import org.eclipse.lsp4j.StaticRegistrationOptions;
import org.eclipse.lsp4j.TextDocumentClientCapabilities;
import org.eclipse.lsp4j.TypeDefinitionParams;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.lxtk.LanguageService;
import org.lxtk.TypeDefinitionProvider;
import org.lxtk.util.Disposable;

/**
 * Participates in a given {@link LanguageService} by implementing and
 * dynamically contributing {@link TypeDefinitionProvider}s according to LSP.
 * <p>
 * This implementation is thread-safe.
 * </p>
 */
public final class TypeDefinitionFeature
    extends TextDocumentLanguageFeature<StaticRegistrationOptions>
{
    private static final String METHOD = "textDocument/typeDefinition"; //$NON-NLS-1$
    private static final Set<String> METHODS = Collections.singleton(METHOD);

    /**
     * Constructor.
     *
     * @param languageService not <code>null</code>
     */
    public TypeDefinitionFeature(LanguageService languageService)
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
        capabilities.setTypeDefinition(getLanguageService().getTypeDefinitionCapabilities());
    }

    @Override
    void initialize(ServerCapabilities capabilities, List<DocumentFilter> documentSelector)
    {
        Either<Boolean, StaticRegistrationOptions> capability =
            capabilities.getTypeDefinitionProvider();
        if (capability == null
            || !(capability.isRight() || Boolean.TRUE.equals(capability.getLeft())))
            return;

        StaticRegistrationOptions registerOptions = new StaticRegistrationOptions();
        StaticRegistrationOptions options = capability.getRight();
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
    Class<StaticRegistrationOptions> getRegistrationOptionsClass()
    {
        return StaticRegistrationOptions.class;
    }

    @Override
    Disposable registerLanguageFeatureProvider(String method, StaticRegistrationOptions options)
    {
        return getLanguageService().getTypeDefinitionProviders().add(new TypeDefinitionProvider()
        {
            @Override
            public StaticRegistrationOptions getRegistrationOptions()
            {
                return options;
            }

            @Override
            public CompletableFuture<
                Either<List<? extends Location>, List<? extends LocationLink>>> getTypeDefinition(
                    TypeDefinitionParams params)
            {
                return getLanguageServer().getTextDocumentService().typeDefinition(params);
            }
        });
    }
}
