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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.eclipse.lsp4j.ClientCapabilities;
import org.eclipse.lsp4j.DocumentFilter;
import org.eclipse.lsp4j.Registration;
import org.eclipse.lsp4j.ServerCapabilities;
import org.eclipse.lsp4j.TextDocumentClientCapabilities;
import org.eclipse.lsp4j.TextDocumentRegistrationOptions;
import org.eclipse.lsp4j.Unregistration;
import org.eclipse.lsp4j.services.LanguageServer;
import org.lxtk.LanguageService;
import org.lxtk.util.Disposable;

abstract class LanguageFeature
    implements DynamicFeature<LanguageServer>
{
    private final LanguageService languageService;
    private LanguageServer languageServer;
    private Map<String, Disposable> registrations;

    /**
     * TODO JavaDoc
     *
     * @param languageService not <code>null</code>
     */
    public LanguageFeature(LanguageService languageService)
    {
        this.languageService = Objects.requireNonNull(languageService);
    }

    /**
     * TODO JavaDoc
     *
     * @return the language service (never <code>null</code>)
     */
    protected final LanguageService getLanguageService()
    {
        return languageService;
    }

    @Override
    public final void fillClientCapabilities(ClientCapabilities capabilities)
    {
        fillClientCapabilities(ClientCapabilitiesUtil.getOrCreateTextDocument(
            capabilities));
    }

    /**
     * TODO JavaDoc
     *
     * @param capabilities never <code>null</code>
     */
    protected abstract void fillClientCapabilities(
        TextDocumentClientCapabilities capabilities);

    @Override
    public final synchronized void initialize(LanguageServer server,
        ServerCapabilities capabilities, List<DocumentFilter> documentSelector)
    {
        languageServer = server;
        registrations = new HashMap<>();
        initialize(capabilities, documentSelector);
    }

    /**
     * TODO JavaDoc
     *
     * @return the language server (never <code>null</code>)
     */
    protected final LanguageServer getLanguageServer()
    {
        if (languageServer == null)
            throw new IllegalStateException();
        return languageServer;
    }

    /**
     * TODO JavaDoc
     *
     * @param capabilities not <code>null</code>
     * @param documentSelector a default document selector, or <code>null</code>
     */
    protected abstract void initialize(ServerCapabilities capabilities,
        List<DocumentFilter> documentSelector);

    @Override
    public final synchronized void register(Registration registration)
    {
        if (!getMethods().contains(registration.getMethod()))
            throw new IllegalArgumentException();

        TextDocumentRegistrationOptions options =
            (TextDocumentRegistrationOptions)registration.getRegisterOptions();
        if (options == null || options.getDocumentSelector() == null)
            return;

        if (registrations == null)
            return;

        if (registrations.containsKey(registration.getId()))
            throw new IllegalArgumentException();

        Disposable disposable = registerLanguageFeatureProvider(
            registration.getMethod(), options);
        if (disposable == null)
            return;

        registrations.put(registration.getId(), disposable);
    }

    /**
     * TODO JavaDoc
     *
     * @param method one of the methods supported by the feature
     *  (never <code>null</code>)
     * @param options the corresponding registration options
     *  (never <code>null</code>)
     * @return a disposable to remove the registered provider,
     *  or <code>null</code> if no provider was registered
     */
    protected abstract Disposable registerLanguageFeatureProvider(String method,
        TextDocumentRegistrationOptions options);

    @Override
    public final synchronized void unregister(Unregistration unregistration)
    {
        if (registrations == null)
            return;

        Disposable registration = registrations.remove(unregistration.getId());
        if (registration != null)
            registration.dispose();
    }

    @Override
    public final synchronized void dispose()
    {
        if (registrations == null)
            return;
        try
        {
            Disposable.disposeAll(registrations.values());
        }
        finally
        {
            registrations = null;
        }
    }
}
