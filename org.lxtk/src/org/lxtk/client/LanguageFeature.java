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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.eclipse.lsp4j.DocumentFilter;
import org.eclipse.lsp4j.InitializeResult;
import org.eclipse.lsp4j.Registration;
import org.eclipse.lsp4j.ServerCapabilities;
import org.eclipse.lsp4j.Unregistration;
import org.eclipse.lsp4j.services.LanguageServer;
import org.lxtk.LanguageFeatureProvider;
import org.lxtk.LanguageService;
import org.lxtk.jsonrpc.DefaultGson;
import org.lxtk.util.Disposable;

import com.google.gson.JsonElement;

abstract class LanguageFeature<RO>
    implements DynamicFeature<LanguageServer>
{
    private final LanguageService languageService;
    private LanguageServer languageServer;
    private Map<String, Disposable> registrations;

    /**
     * Constructor.
     *
     * @param languageService not <code>null</code>
     */
    public LanguageFeature(LanguageService languageService)
    {
        this.languageService = Objects.requireNonNull(languageService);
    }

    /**
     * Returns the language service for this feature.
     *
     * @return the language service (never <code>null</code>)
     */
    final LanguageService getLanguageService()
    {
        return languageService;
    }

    @Override
    public final synchronized void initialize(LanguageServer server,
        InitializeResult initializeResult, List<DocumentFilter> documentSelector)
    {
        languageServer = server;
        registrations = new HashMap<>();
        initialize(initializeResult.getCapabilities(), documentSelector);
    }

    /**
     * Returns the language server for this feature.
     *
     * @return the language server (never <code>null</code>)
     * @throws IllegalStateException if this feature has not been {@link
     *  Feature#initialize(LanguageServer, ServerCapabilities, List)
     *  initialized}
     */
    final LanguageServer getLanguageServer()
    {
        if (languageServer == null)
            throw new IllegalStateException();
        return languageServer;
    }

    /**
     * Initialization callback.
     *
     * @param capabilities not <code>null</code>
     * @param documentSelector a default document selector, or <code>null</code>
     */
    abstract void initialize(ServerCapabilities capabilities,
        List<DocumentFilter> documentSelector);

    @Override
    public final synchronized void register(Registration registration)
    {
        if (!getMethods().contains(registration.getMethod()))
            throw new IllegalArgumentException();

        Object rO = registration.getRegisterOptions();
        Class<RO> optionsClass = getRegistrationOptionsClass();
        RO options = rO instanceof JsonElement
            ? DefaultGson.INSTANCE.fromJson((JsonElement)rO, optionsClass) : optionsClass.cast(rO);
        if (!checkRegistrationOptions(options))
            return;

        if (registrations == null)
            return;

        if (registrations.containsKey(registration.getId()))
            throw new IllegalArgumentException();

        Disposable disposable = registerLanguageFeatureProvider(registration.getMethod(), options);
        if (disposable == null)
            return;

        registrations.put(registration.getId(), disposable);
    }

    /**
     * Returns the registration options class.
     *
     * @return the registration options class (never <code>null</code>)
     */
    abstract Class<RO> getRegistrationOptionsClass();

    /**
     * Checks the given registration options.
     *
     * @param options may be <code>null</code>
     * @return <code>true</code> if the given options are valid,
     *  and <code>false</code> otherwise
     */
    abstract boolean checkRegistrationOptions(RO options);

    /**
     * Contributes a {@link LanguageFeatureProvider} to the language service.
     *
     * @param method one of the methods supported by the feature
     *  (never <code>null</code>)
     * @param options the corresponding registration options,
     *  checked by {@link #checkRegistrationOptions(RO)}
     * @return a disposable to remove the registered provider,
     *  or <code>null</code> if no provider was registered
     */
    abstract Disposable registerLanguageFeatureProvider(String method, RO options);

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
