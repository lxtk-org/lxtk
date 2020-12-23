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
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.eclipse.lsp4j.CodeLens;
import org.eclipse.lsp4j.CodeLensCapabilities;
import org.eclipse.lsp4j.CodeLensOptions;
import org.eclipse.lsp4j.CodeLensParams;
import org.eclipse.lsp4j.CodeLensRegistrationOptions;
import org.eclipse.lsp4j.DocumentFilter;
import org.eclipse.lsp4j.Registration;
import org.eclipse.lsp4j.ServerCapabilities;
import org.eclipse.lsp4j.TextDocumentClientCapabilities;
import org.lxtk.CodeLensProvider;
import org.lxtk.CommandService;
import org.lxtk.LanguageService;
import org.lxtk.util.Disposable;

/**
 * Participates in a given {@link LanguageService} by implementing and
 * dynamically contributing {@link CodeLensProvider}s according to LSP.
 * <p>
 * This implementation is thread-safe.
 * </p>
 */
public final class CodeLensFeature
    extends TextDocumentLanguageFeature<CodeLensRegistrationOptions>
{
    private static final String METHOD = "textDocument/codeLens"; //$NON-NLS-1$
    private static final Set<String> METHODS = Collections.singleton(METHOD);

    private final CommandService commandService;

    /**
     * Constructor.
     *
     * @param languageService not <code>null</code>
     * @param commandService not <code>null</code>
     */
    public CodeLensFeature(LanguageService languageService, CommandService commandService)
    {
        super(languageService);
        this.commandService = Objects.requireNonNull(commandService);
    }

    @Override
    public Set<String> getMethods()
    {
        return METHODS;
    }

    @Override
    void fillClientCapabilities(TextDocumentClientCapabilities capabilities)
    {
        CodeLensCapabilities codeLens = getLanguageService().getCodeLensCapabilities();
        codeLens.setDynamicRegistration(true);
        capabilities.setCodeLens(codeLens);
    }

    @Override
    void initialize(ServerCapabilities capabilities, List<DocumentFilter> documentSelector)
    {
        if (documentSelector == null)
            return;

        CodeLensOptions capability = capabilities.getCodeLensProvider();
        if (capability == null)
            return;

        CodeLensRegistrationOptions options = new CodeLensRegistrationOptions();
        options.setDocumentSelector(documentSelector);
        options.setResolveProvider(capability.isResolveProvider());
        register(new Registration(UUID.randomUUID().toString(), METHOD, options));
    }

    @Override
    Class<CodeLensRegistrationOptions> getRegistrationOptionsClass()
    {
        return CodeLensRegistrationOptions.class;
    }

    @Override
    Disposable registerLanguageFeatureProvider(String method, CodeLensRegistrationOptions options)
    {
        return getLanguageService().getCodeLensProviders().add(new CodeLensProvider()
        {
            @Override
            public CodeLensRegistrationOptions getRegistrationOptions()
            {
                return options;
            }

            @Override
            public CompletableFuture<List<? extends CodeLens>> getCodeLenses(CodeLensParams params)
            {
                return getLanguageServer().getTextDocumentService().codeLens(params);
            }

            @Override
            public CompletableFuture<CodeLens> resolveCodeLens(CodeLens unresolved)
            {
                if (!Boolean.TRUE.equals(options.getResolveProvider()))
                    throw new UnsupportedOperationException();

                return getLanguageServer().getTextDocumentService().resolveCodeLens(unresolved);
            }

            @Override
            public CommandService getCommandService()
            {
                return commandService;
            }
        });
    }
}
