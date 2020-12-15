/*******************************************************************************
 * Copyright (c) 2019, 2020 1C-Soft LLC.
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
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.ReferenceParams;
import org.eclipse.lsp4j.ReferenceRegistrationOptions;
import org.eclipse.lsp4j.ReferencesCapabilities;
import org.eclipse.lsp4j.Registration;
import org.eclipse.lsp4j.ServerCapabilities;
import org.eclipse.lsp4j.TextDocumentClientCapabilities;
import org.lxtk.LanguageService;
import org.lxtk.ProgressService;
import org.lxtk.ReferenceProvider;
import org.lxtk.util.Disposable;

/**
 * Participates in a given {@link LanguageService} by implementing and
 * dynamically contributing {@link ReferenceProvider}s according to LSP.
 * <p>
 * This implementation is thread-safe.
 * </p>
 */
public final class ReferencesFeature
    extends TextDocumentLanguageFeature<ReferenceRegistrationOptions>
{
    private static final String METHOD = "textDocument/references"; //$NON-NLS-1$
    private static final Set<String> METHODS = Collections.singleton(METHOD);

    /**
     * Constructor.
     *
     * @param languageService not <code>null</code>
     */
    public ReferencesFeature(LanguageService languageService)
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
        ReferencesCapabilities references = getLanguageService().getReferencesCapabilities();
        references.setDynamicRegistration(true);
        capabilities.setReferences(references);
    }

    @Override
    void initialize(ServerCapabilities capabilities, List<DocumentFilter> documentSelector)
    {
        if (documentSelector == null)
            return;

        Boolean capability = capabilities.getReferencesProvider();
        if (!Boolean.TRUE.equals(capability))
            return;

        ReferenceRegistrationOptions registerOptions = new ReferenceRegistrationOptions();
        registerOptions.setDocumentSelector(documentSelector);
        // TODO registerOptions.setWorkDoneProgress(workDoneProgress);

        register(new Registration(UUID.randomUUID().toString(), METHOD, registerOptions));
    }

    @Override
    Class<ReferenceRegistrationOptions> getRegistrationOptionsClass()
    {
        return ReferenceRegistrationOptions.class;
    }

    @Override
    Disposable registerLanguageFeatureProvider(String method, ReferenceRegistrationOptions options)
    {
        return getLanguageService().getReferenceProviders().add(new ReferenceProvider()
        {
            @Override
            public ReferenceRegistrationOptions getRegistrationOptions()
            {
                return options;
            }

            @Override
            public ProgressService getProgressService()
            {
                return getLanguageClient().getProgressService();
            }

            @Override
            public CompletableFuture<List<? extends Location>> getReferences(ReferenceParams params)
            {
                return getLanguageServer().getTextDocumentService().references(params);
            }
        });
    }
}
