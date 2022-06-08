/*******************************************************************************
 * Copyright (c) 2020, 2022 1C-Soft LLC.
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

import org.eclipse.lsp4j.ClientCapabilities;
import org.eclipse.lsp4j.DocumentFilter;
import org.eclipse.lsp4j.Registration;
import org.eclipse.lsp4j.ServerCapabilities;
import org.eclipse.lsp4j.SymbolCapabilities;
import org.eclipse.lsp4j.WorkspaceSymbol;
import org.eclipse.lsp4j.WorkspaceSymbolOptions;
import org.eclipse.lsp4j.WorkspaceSymbolParams;
import org.eclipse.lsp4j.WorkspaceSymbolRegistrationOptions;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.lxtk.LanguageService;
import org.lxtk.ProgressService;
import org.lxtk.WorkspaceSymbolProvider;
import org.lxtk.util.Disposable;

/**
 * Participates in a given {@link LanguageService} by implementing and
 * dynamically contributing {@link WorkspaceSymbolProvider}s according to LSP.
 * <p>
 * This implementation is thread-safe.
 * </p>
 */
public final class WorkspaceSymbolFeature
    extends LanguageFeature<WorkspaceSymbolRegistrationOptions>
{
    private static final String METHOD = "workspace/symbol"; //$NON-NLS-1$
    private static final Set<String> METHODS = Collections.singleton(METHOD);

    private final Object context;

    /**
     * Constructor.
     *
     * @param languageService not <code>null</code>
     * @param context may be <code>null</code>
     */
    public WorkspaceSymbolFeature(LanguageService languageService, Object context)
    {
        super(languageService);
        this.context = context;
    }

    @Override
    public Set<String> getMethods()
    {
        return METHODS;
    }

    @Override
    public void fillClientCapabilities(ClientCapabilities capabilities)
    {
        SymbolCapabilities symbol = getLanguageService().getWorkspaceSymbolCapabilities();
        symbol.setDynamicRegistration(true);
        ClientCapabilitiesUtil.getOrCreateWorkspace(capabilities).setSymbol(symbol);
    }

    @Override
    void initialize(ServerCapabilities capabilities, List<DocumentFilter> documentSelector)
    {
        Either<Boolean, WorkspaceSymbolOptions> capability =
            capabilities.getWorkspaceSymbolProvider();
        if (capability == null
            || !(capability.isRight() || Boolean.TRUE.equals(capability.getLeft())))
            return;

        WorkspaceSymbolRegistrationOptions registerOptions =
            new WorkspaceSymbolRegistrationOptions();

        WorkspaceSymbolOptions options = capability.getRight();
        if (options != null)
        {
            registerOptions.setWorkDoneProgress(options.getWorkDoneProgress());
        }

        register(new Registration(UUID.randomUUID().toString(), METHOD, registerOptions));
    }

    @Override
    Class<WorkspaceSymbolRegistrationOptions> getRegistrationOptionsClass()
    {
        return WorkspaceSymbolRegistrationOptions.class;
    }

    @Override
    boolean checkRegistrationOptions(WorkspaceSymbolRegistrationOptions options)
    {
        return options != null;
    }

    @Override
    Disposable registerLanguageFeatureProvider(String method,
        WorkspaceSymbolRegistrationOptions options)
    {
        return getLanguageService().getWorkspaceSymbolProviders().add(new WorkspaceSymbolProvider()
        {
            @Override
            public WorkspaceSymbolRegistrationOptions getRegistrationOptions()
            {
                return options;
            }

            @Override
            public ProgressService getProgressService()
            {
                return getLanguageClient().getProgressService();
            }

            @Override
            public Object getContext()
            {
                return context;
            }

            @SuppressWarnings("deprecation")
            @Override
            public CompletableFuture<Either<List<? extends org.eclipse.lsp4j.SymbolInformation>,
                List<? extends WorkspaceSymbol>>> getWorkspaceSymbols(WorkspaceSymbolParams params)
            {
                return getLanguageServer().getWorkspaceService().symbol(params);
            }
        });
    }
}
