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

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import org.eclipse.lsp4j.ClientCapabilities;
import org.eclipse.lsp4j.ClientInfo;
import org.eclipse.lsp4j.DocumentFilter;
import org.eclipse.lsp4j.InitializeParams;
import org.eclipse.lsp4j.InitializeResult;
import org.eclipse.lsp4j.MessageParams;
import org.eclipse.lsp4j.ProgressParams;
import org.eclipse.lsp4j.PublishDiagnosticsParams;
import org.eclipse.lsp4j.Registration;
import org.eclipse.lsp4j.RegistrationParams;
import org.eclipse.lsp4j.ServerInfo;
import org.eclipse.lsp4j.Unregistration;
import org.eclipse.lsp4j.UnregistrationParams;
import org.eclipse.lsp4j.WorkspaceFolder;
import org.eclipse.lsp4j.jsonrpc.Endpoint;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.services.LanguageServer;
import org.lxtk.ProgressService;
import org.lxtk.WorkspaceService;
import org.lxtk.jsonrpc.DefaultGson;
import org.lxtk.util.Disposable;
import org.lxtk.util.Log;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * Partial implementation of a {@link LanguageClient} that is also a composite
 * {@link Feature}.
 *
 * @param <S> server interface type
*/
public abstract class AbstractLanguageClient<S extends LanguageServer>
    implements LanguageClient, Feature<S>
{
    private static final String DOCUMENT_SELECTOR = "documentSelector"; //$NON-NLS-1$

    private final Log log;
    private final Consumer<PublishDiagnosticsParams> diagnosticConsumer;
    private final Set<Feature<? super S>> featureSet;
    private final Map<String, DynamicFeature<? super S>> dynamicFeatures = new HashMap<>();
    private S languageServer;
    private ServerInfo serverInfo;
    private List<DocumentFilter> documentSelector;

    /**
     * Constructor.
     *
     * @param log the client's log (not <code>null</code>)
     * @param diagnosticConsumer the client's diagnostic consumer
     *  (not <code>null</code>)
     * @param features the client's features (not <code>null</code>).
     *  Subsequent modifications of the given collection will have no effect
     *  on the constructed instance
     */
    public AbstractLanguageClient(Log log, Consumer<PublishDiagnosticsParams> diagnosticConsumer,
        Collection<Feature<? super S>> features)
    {
        this.log = Objects.requireNonNull(log);
        this.diagnosticConsumer = Objects.requireNonNull(diagnosticConsumer);
        featureSet = new LinkedHashSet<>(features);
        for (Feature<? super S> feature : featureSet)
        {
            feature.setLanguageClient(this);
            if (feature instanceof DynamicFeature)
            {
                DynamicFeature<? super S> dynamicFeature = (DynamicFeature<? super S>)feature;
                Set<String> methods = dynamicFeature.getMethods();
                if (methods.isEmpty())
                    throw new IllegalArgumentException(
                        "Dynamic feature must support at least one method."); //$NON-NLS-1$
                if (!Collections.disjoint(methods, dynamicFeatures.keySet()))
                    throw new IllegalArgumentException(
                        "Dynamic features must have no methods in common."); //$NON-NLS-1$
                for (String method : methods)
                    dynamicFeatures.put(method, dynamicFeature);
            }
        }
    }

    /**
     * Returns the locale the client currently displays the user interface in.
     * This need not necessarily be the locale of the operating system.
     *
     * @return the client locale (never <code>null</code>)
     */
    public Locale getLocale()
    {
        return Locale.getDefault(Locale.Category.DISPLAY);
    }

    /**
     * Returns the client info.
     *
     * @return the client info, or <code>null</code> if none
     */
    public ClientInfo getClientInfo()
    {
        return null;
    }

    /**
     * Returns the server info.
     *
     * @return the server info, or <code>null</code> if none
     */
    public final ServerInfo getServerInfo()
    {
        return serverInfo;
    }

    /**
     * Returns the workspace service associated with this client.
     *
     * @return the associated workspace service, or <code>null</code> if none
     */
    public WorkspaceService getWorkspaceService()
    {
        return null;
    }

    /**
     * Returns the progress service associated with this client.
     *
     * @return the associated progress service, or <code>null</code> if none
     */
    public ProgressService getProgressService()
    {
        return null;
    }

    @Override
    public void fillInitializeParams(InitializeParams params)
    {
        params.setLocale(getLocale().toLanguageTag());

        params.setClientInfo(getClientInfo());

        for (Feature<? super S> feature : featureSet)
        {
            feature.fillInitializeParams(params);
        }
    }

    @Override
    public void fillClientCapabilities(ClientCapabilities capabilities)
    {
        for (Feature<? super S> feature : featureSet)
        {
            feature.fillClientCapabilities(capabilities);
        }
    }

    @Override
    public Endpoint adviseServerEndpoint(Endpoint endpoint)
    {
        for (Feature<? super S> feature : featureSet)
        {
            endpoint = feature.adviseServerEndpoint(endpoint);
        }
        return endpoint;
    }

    @Override
    public void initialize(S languageServer, InitializeResult initializeResult,
        List<DocumentFilter> documentSelector)
    {
        this.languageServer = languageServer;
        this.serverInfo = initializeResult.getServerInfo();
        this.documentSelector = documentSelector;

        for (Feature<? super S> feature : featureSet)
        {
            feature.initialize(languageServer, initializeResult, documentSelector);
        }
    }

    @Override
    public CompletableFuture<Void> registerCapability(RegistrationParams params)
    {
        return CompletableFuture.runAsync(() ->
        {
            for (Registration registration : params.getRegistrations())
            {
                DynamicFeature<? super S> feature = dynamicFeatures.get(registration.getMethod());
                if (feature == null)
                    throw new IllegalStateException("No feature implementation is found for method " //$NON-NLS-1$
                        + registration.getMethod());
                Object registerOptions = registration.getRegisterOptions();
                if (registerOptions instanceof JsonObject)
                {
                    JsonObject rO = ((JsonObject)registerOptions);
                    if (rO.has(DOCUMENT_SELECTOR))
                    {
                        JsonElement dS = rO.get(DOCUMENT_SELECTOR);
                        if (dS.isJsonNull() && documentSelector != null)
                        {
                            rO.remove(DOCUMENT_SELECTOR);
                            rO.add(DOCUMENT_SELECTOR,
                                DefaultGson.INSTANCE.toJsonTree(documentSelector));
                        }
                    }
                }
                feature.register(registration);
            }
        });
    }

    @Override
    public CompletableFuture<Void> unregisterCapability(UnregistrationParams params)
    {
        return CompletableFuture.runAsync(() ->
        {
            for (Unregistration unregistration : params.getUnregisterations())
            {
                DynamicFeature<? super S> feature = dynamicFeatures.get(unregistration.getMethod());
                if (feature == null)
                    throw new IllegalStateException("No feature implementation is found for method " //$NON-NLS-1$
                        + unregistration.getMethod());
                feature.unregister(unregistration);
            }
        });
    }

    @Override
    public void publishDiagnostics(PublishDiagnosticsParams params)
    {
        diagnosticConsumer.accept(params);
    }

    @Override
    public void logMessage(MessageParams params)
    {
        switch (params.getType())
        {
        case Error:
            log().error(params.getMessage());
            break;
        case Warning:
            log().warning(params.getMessage());
            break;
        default:
            log().info(params.getMessage());
        }
    }

    @Override
    public CompletableFuture<List<WorkspaceFolder>> workspaceFolders()
    {
        return CompletableFuture.supplyAsync(() ->
        {
            WorkspaceService workspaceService = getWorkspaceService();
            if (workspaceService == null)
                throw new UnsupportedOperationException();
            return org.lxtk.WorkspaceFolder.toProtocol(workspaceService.getWorkspaceFolders());
        });
    }

    @Override
    public void notifyProgress(ProgressParams params)
    {
        ProgressService progressService = getProgressService();
        if (progressService != null)
            progressService.accept(params);
    }

    @Override
    public void dispose()
    {
        Disposable.disposeAll(featureSet);
    }

    /**
     * Returns the language server of this client.
     *
     * @return the language server (never <code>null</code>)
     * @throws IllegalStateException if the client has not been {@link
     *  #initialize(LanguageServer, InitializeResult, List) initialized}
     */
    protected final S getLanguageServer()
    {
        if (languageServer == null)
            throw new IllegalStateException();
        return languageServer;
    }

    /**
     * Returns the log associated with this client.
     *
     * @return the client's log (never <code>null</code>)
     */
    protected final Log log()
    {
        return log;
    }
}
