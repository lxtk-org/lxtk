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

import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

import org.eclipse.lsp4j.ClientCapabilities;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DocumentFilter;
import org.eclipse.lsp4j.InitializeParams;
import org.eclipse.lsp4j.MessageParams;
import org.eclipse.lsp4j.PublishDiagnosticsParams;
import org.eclipse.lsp4j.Registration;
import org.eclipse.lsp4j.RegistrationParams;
import org.eclipse.lsp4j.ServerCapabilities;
import org.eclipse.lsp4j.TextDocumentRegistrationOptions;
import org.eclipse.lsp4j.Unregistration;
import org.eclipse.lsp4j.UnregistrationParams;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.services.LanguageServer;
import org.lxtk.DocumentUri;
import org.lxtk.util.Disposable;
import org.lxtk.util.Log;

/**
 * Partial implementation of a {@link LanguageClient} that is also a composite
 * {@link Feature}.
 *
 * @param <S> server interface type
*/
public abstract class AbstractLanguageClient<S extends LanguageServer>
    implements LanguageClient, Feature<S>
{
    private final Log log;
    private final BiConsumer<URI, Collection<Diagnostic>> diagnosticConsumer;
    private final Set<Feature<? super S>> featureSet;
    private final Map<String, DynamicFeature<? super S>> dynamicFeatures =
        new HashMap<>();
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
    public AbstractLanguageClient(Log log,
        BiConsumer<URI, Collection<Diagnostic>> diagnosticConsumer,
        Collection<Feature<? super S>> features)
    {
        this.log = Objects.requireNonNull(log);
        this.diagnosticConsumer = Objects.requireNonNull(diagnosticConsumer);
        featureSet = new LinkedHashSet<>(features);
        for (Feature<? super S> feature : featureSet)
        {
            if (feature instanceof DynamicFeature)
            {
                DynamicFeature<? super S> dynamicFeature =
                    (DynamicFeature<? super S>)feature;
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

    @Override
    public void fillInitializeParams(InitializeParams params)
    {
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
    public void initialize(S server, ServerCapabilities capabilities,
        List<DocumentFilter> documentSelector)
    {
        this.documentSelector = documentSelector;
        for (Feature<? super S> feature : featureSet)
        {
            feature.initialize(server, capabilities, documentSelector);
        }
    }

    @Override
    public CompletableFuture<Void> registerCapability(RegistrationParams params)
    {
        return CompletableFuture.runAsync(() ->
        {
            for (Registration registration : params.getRegistrations())
            {
                DynamicFeature<? super S> feature = dynamicFeatures.get(
                    registration.getMethod());
                if (feature == null)
                    throw new IllegalStateException(
                        "No feature implementation is found for method " //$NON-NLS-1$
                            + registration.getMethod());
                Object registerOptions = registration.getRegisterOptions();
                if (registerOptions instanceof TextDocumentRegistrationOptions)
                {
                    TextDocumentRegistrationOptions options =
                        (TextDocumentRegistrationOptions)registerOptions;
                    if (options.getDocumentSelector() == null)
                        options.setDocumentSelector(documentSelector);
                }
                feature.register(registration);
            }
        });
    }

    @Override
    public CompletableFuture<Void> unregisterCapability(
        UnregistrationParams params)
    {
        return CompletableFuture.runAsync(() ->
        {
            for (Unregistration unregistration : params.getUnregisterations())
            {
                DynamicFeature<? super S> feature = dynamicFeatures.get(
                    unregistration.getMethod());
                if (feature == null)
                    throw new IllegalStateException(
                        "No feature implementation is found for method " //$NON-NLS-1$
                            + unregistration.getMethod());
                feature.unregister(unregistration);
            }
        });
    }

    @Override
    public void publishDiagnostics(PublishDiagnosticsParams params)
    {
        diagnosticConsumer.accept(DocumentUri.convert(params.getUri()),
            params.getDiagnostics());
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
    public void dispose()
    {
        Disposable.disposeAll(featureSet);
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
