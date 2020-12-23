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
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.eclipse.lsp4j.ClientCapabilities;
import org.eclipse.lsp4j.DocumentFilter;
import org.eclipse.lsp4j.Registration;
import org.eclipse.lsp4j.ServerCapabilities;
import org.eclipse.lsp4j.SymbolCapabilities;
import org.eclipse.lsp4j.SymbolInformation;
import org.eclipse.lsp4j.WorkspaceSymbolParams;
import org.lxtk.LanguageService;
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
    extends LanguageFeature<Object>
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
        Boolean capability = capabilities.getWorkspaceSymbolProvider();
        if (!Boolean.TRUE.equals(capability))
            return;

        register(new Registration(UUID.randomUUID().toString(), METHOD));
    }

    @Override
    Class<Object> getRegistrationOptionsClass()
    {
        return Object.class;
    }

    @Override
    boolean checkRegistrationOptions(Object options)
    {
        return true;
    }

    @Override
    Disposable registerLanguageFeatureProvider(String method, Object options)
    {
        return getLanguageService().getWorkspaceSymbolProviders().add(new WorkspaceSymbolProvider()
        {
            @Override
            public Object getRegistrationOptions()
            {
                return options;
            }

            @Override
            public Object getContext()
            {
                return context;
            }

            @Override
            public CompletableFuture<List<? extends SymbolInformation>> getWorkspaceSymbols(
                WorkspaceSymbolParams params)
            {
                return getLanguageServer().getWorkspaceService().symbol(params);
            }
        });
    }
}
