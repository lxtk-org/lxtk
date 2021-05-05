/*******************************************************************************
 * Copyright (c) 2021 1C-Soft LLC.
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

import org.eclipse.lsp4j.CallHierarchyCapabilities;
import org.eclipse.lsp4j.CallHierarchyIncomingCall;
import org.eclipse.lsp4j.CallHierarchyIncomingCallsParams;
import org.eclipse.lsp4j.CallHierarchyItem;
import org.eclipse.lsp4j.CallHierarchyOutgoingCall;
import org.eclipse.lsp4j.CallHierarchyOutgoingCallsParams;
import org.eclipse.lsp4j.CallHierarchyPrepareParams;
import org.eclipse.lsp4j.CallHierarchyRegistrationOptions;
import org.eclipse.lsp4j.DocumentFilter;
import org.eclipse.lsp4j.Registration;
import org.eclipse.lsp4j.ServerCapabilities;
import org.eclipse.lsp4j.TextDocumentClientCapabilities;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.lxtk.CallHierarchyProvider;
import org.lxtk.LanguageService;
import org.lxtk.ProgressService;
import org.lxtk.util.Disposable;

/**
 * Participates in a given {@link LanguageService} by implementing and
 * dynamically contributing {@link CallHierarchyProvider}s according to LSP.
 * <p>
 * This implementation is thread-safe.
 * </p>
 */
public class CallHierarchyFeature
    extends TextDocumentLanguageFeature<CallHierarchyRegistrationOptions>
{
    private static final String METHOD = "textDocument/prepareCallHierarchy"; //$NON-NLS-1$
    private static final Set<String> METHODS = Collections.singleton(METHOD);

    /**
     * Constructor.
     *
     * @param languageService not <code>null</code>
     */
    public CallHierarchyFeature(LanguageService languageService)
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
        CallHierarchyCapabilities callHierarchy =
            getLanguageService().getCallHierarchyCapabilities();
        callHierarchy.setDynamicRegistration(true);
        capabilities.setCallHierarchy(callHierarchy);
    }

    @Override
    void initialize(ServerCapabilities capabilities, List<DocumentFilter> documentSelector)
    {
        Either<Boolean, CallHierarchyRegistrationOptions> capability =
            capabilities.getCallHierarchyProvider();
        if (capability == null
            || !(capability.isRight() || Boolean.TRUE.equals(capability.getLeft())))
            return;

        CallHierarchyRegistrationOptions registerOptions = new CallHierarchyRegistrationOptions();
        CallHierarchyRegistrationOptions options = capability.getRight();
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
    Class<CallHierarchyRegistrationOptions> getRegistrationOptionsClass()
    {
        return CallHierarchyRegistrationOptions.class;
    }

    @Override
    Disposable registerLanguageFeatureProvider(String method,
        CallHierarchyRegistrationOptions options)
    {
        return getLanguageService().getCallHierarchyProviders().add(new CallHierarchyProvider()
        {
            @Override
            public CallHierarchyRegistrationOptions getRegistrationOptions()
            {
                return options;
            }

            @Override
            public ProgressService getProgressService()
            {
                return getLanguageClient().getProgressService();
            }

            @Override
            public CompletableFuture<List<CallHierarchyItem>> prepareCallHierarchy(
                CallHierarchyPrepareParams params)
            {
                return getLanguageServer().getTextDocumentService().prepareCallHierarchy(params);
            }

            @Override
            public CompletableFuture<List<CallHierarchyIncomingCall>> getCallHierarchyIncomingCalls(
                CallHierarchyIncomingCallsParams params)
            {
                return getLanguageServer().getTextDocumentService().callHierarchyIncomingCalls(
                    params);
            }

            @Override
            public CompletableFuture<List<CallHierarchyOutgoingCall>> getCallHierarchyOutgoingCalls(
                CallHierarchyOutgoingCallsParams params)
            {
                return getLanguageServer().getTextDocumentService().callHierarchyOutgoingCalls(
                    params);
            }
        });
    }
}
