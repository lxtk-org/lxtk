/*******************************************************************************
 * Copyright (c) 2022 1C-Soft LLC.
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
import org.eclipse.lsp4j.Registration;
import org.eclipse.lsp4j.ServerCapabilities;
import org.eclipse.lsp4j.TextDocumentClientCapabilities;
import org.eclipse.lsp4j.TypeHierarchyCapabilities;
import org.eclipse.lsp4j.TypeHierarchyItem;
import org.eclipse.lsp4j.TypeHierarchyPrepareParams;
import org.eclipse.lsp4j.TypeHierarchyRegistrationOptions;
import org.eclipse.lsp4j.TypeHierarchySubtypesParams;
import org.eclipse.lsp4j.TypeHierarchySupertypesParams;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.lxtk.LanguageService;
import org.lxtk.ProgressService;
import org.lxtk.TypeHierarchyProvider;
import org.lxtk.util.Disposable;

/**
 * Participates in a given {@link LanguageService} by implementing and
 * dynamically contributing {@link TypeHierarchyProvider}s according to LSP.
 * <p>
 * This implementation is thread-safe.
 * </p>
 */
public class TypeHierarchyFeature
    extends TextDocumentLanguageFeature<TypeHierarchyRegistrationOptions>
{
    private static final String METHOD = "textDocument/prepareTypeHierarchy"; //$NON-NLS-1$
    private static final Set<String> METHODS = Collections.singleton(METHOD);

    /**
     * Constructor.
     *
     * @param languageService not <code>null</code>
     */
    public TypeHierarchyFeature(LanguageService languageService)
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
        TypeHierarchyCapabilities typeHierarchy =
            getLanguageService().getTypeHierarchyCapabilities();
        typeHierarchy.setDynamicRegistration(true);
        capabilities.setTypeHierarchy(typeHierarchy);
    }

    @Override
    void initialize(ServerCapabilities capabilities, List<DocumentFilter> documentSelector)
    {
        Either<Boolean, TypeHierarchyRegistrationOptions> capability =
            capabilities.getTypeHierarchyProvider();
        if (capability == null
            || !(capability.isRight() || Boolean.TRUE.equals(capability.getLeft())))
            return;

        TypeHierarchyRegistrationOptions registerOptions = new TypeHierarchyRegistrationOptions();
        TypeHierarchyRegistrationOptions options = capability.getRight();
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
    Class<TypeHierarchyRegistrationOptions> getRegistrationOptionsClass()
    {
        return TypeHierarchyRegistrationOptions.class;
    }

    @Override
    Disposable registerLanguageFeatureProvider(String method,
        TypeHierarchyRegistrationOptions options)
    {
        return getLanguageService().getTypeHierarchyProviders().add(new TypeHierarchyProvider()
        {
            @Override
            public TypeHierarchyRegistrationOptions getRegistrationOptions()
            {
                return options;
            }

            @Override
            public ProgressService getProgressService()
            {
                return getLanguageClient().getProgressService();
            }

            @Override
            public CompletableFuture<List<TypeHierarchyItem>> prepareTypeHierarchy(
                TypeHierarchyPrepareParams params)
            {
                return getLanguageServer().getTextDocumentService().prepareTypeHierarchy(params);
            }

            @Override
            public CompletableFuture<List<TypeHierarchyItem>> getTypeHierarchySupertypes(
                TypeHierarchySupertypesParams params)
            {
                return getLanguageServer().getTextDocumentService().typeHierarchySupertypes(params);
            }

            @Override
            public CompletableFuture<List<TypeHierarchyItem>> getTypeHierarchySubtypes(
                TypeHierarchySubtypesParams params)
            {
                return getLanguageServer().getTextDocumentService().typeHierarchySubtypes(params);
            }
        });
    }
}
