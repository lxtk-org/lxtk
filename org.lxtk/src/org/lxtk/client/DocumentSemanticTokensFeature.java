/*******************************************************************************
 * Copyright (c) 2021, 2022 1C-Soft LLC.
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
import java.util.function.Consumer;

import org.eclipse.lsp4j.ClientCapabilities;
import org.eclipse.lsp4j.DocumentFilter;
import org.eclipse.lsp4j.Registration;
import org.eclipse.lsp4j.SemanticTokens;
import org.eclipse.lsp4j.SemanticTokensCapabilities;
import org.eclipse.lsp4j.SemanticTokensClientCapabilitiesRequestsFull;
import org.eclipse.lsp4j.SemanticTokensDelta;
import org.eclipse.lsp4j.SemanticTokensDeltaParams;
import org.eclipse.lsp4j.SemanticTokensParams;
import org.eclipse.lsp4j.SemanticTokensRangeParams;
import org.eclipse.lsp4j.SemanticTokensServerFull;
import org.eclipse.lsp4j.SemanticTokensWithRegistrationOptions;
import org.eclipse.lsp4j.SemanticTokensWorkspaceCapabilities;
import org.eclipse.lsp4j.ServerCapabilities;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.services.LanguageServer;
import org.lxtk.DocumentSemanticTokensProvider;
import org.lxtk.LanguageService;
import org.lxtk.ProgressService;
import org.lxtk.util.Disposable;
import org.lxtk.util.EventEmitter;
import org.lxtk.util.EventStream;
import org.lxtk.util.SafeRun;

/**
 * Participates in a given {@link LanguageService} by implementing and dynamically contributing
 * {@link SemanticTokensWithRegistrationOptions}s according to LSP.
 * <p>
 * This implementation is thread-safe.
 * </p>
 */
public final class DocumentSemanticTokensFeature
    extends LanguageFeature<SemanticTokensWithRegistrationOptions>
{
    private static final String METHOD = "textDocument/semanticTokens"; //$NON-NLS-1$
    private static final Set<String> METHODS = Collections.singleton(METHOD);

    private Consumer<Throwable> logger;

    /**
     * Constructor.
     *
     * @param languageService not <code>null</code>
     */
    public DocumentSemanticTokensFeature(LanguageService languageService)
    {
        super(languageService);
    }

    @Override
    public void setLanguageClient(AbstractLanguageClient<? extends LanguageServer> client)
    {
        super.setLanguageClient(client);
        logger = client.log()::error;
    }

    @Override
    public Set<String> getMethods()
    {
        return METHODS;
    }

    @Override
    public void fillClientCapabilities(ClientCapabilities capabilities)
    {
        SemanticTokensCapabilities semanticTokens =
            getLanguageService().getDocumentSemanticTokensCapabilities();
        semanticTokens.setDynamicRegistration(true);
        semanticTokens.getRequests().setFull(
            Either.forRight(new SemanticTokensClientCapabilitiesRequestsFull(true)));
        semanticTokens.getRequests().setRange(true);
        ClientCapabilitiesUtil.getOrCreateTextDocument(capabilities).setSemanticTokens(
            semanticTokens);
        ClientCapabilitiesUtil.getOrCreateWorkspace(capabilities).setSemanticTokens(
            new SemanticTokensWorkspaceCapabilities(true));
    }

    @Override
    void initialize(ServerCapabilities capabilities, List<DocumentFilter> documentSelector)
    {
        SemanticTokensWithRegistrationOptions options = capabilities.getSemanticTokensProvider();
        if (options == null)
            return;

        String id = options.getId();
        if (id == null)
            id = UUID.randomUUID().toString();

        if (options.getDocumentSelector() == null)
            options.setDocumentSelector(documentSelector);

        register(new Registration(id, METHOD, options));
    }

    @Override
    Class<? extends SemanticTokensWithRegistrationOptions> getRegistrationOptionsClass()
    {
        return SemanticTokensWithRegistrationOptions.class;
    }

    @Override
    boolean checkRegistrationOptions(SemanticTokensWithRegistrationOptions options)
    {
        return options != null && options.getDocumentSelector() != null;
    }

    @Override
    Disposable registerLanguageFeatureProvider(String method,
        SemanticTokensWithRegistrationOptions options)
    {
        EventEmitter<Void> onRefreshSemanticTokens = new EventEmitter<>();
        return SafeRun.runWithResult(rollback ->
        {
            Disposable registration = getLanguageService().getDocumentSemanticTokensProviders().add(
                new DocumentSemanticTokensProvider()
                {
                    @Override
                    public SemanticTokensWithRegistrationOptions getRegistrationOptions()
                    {
                        return options;
                    }

                    @Override
                    public ProgressService getProgressService()
                    {
                        return getLanguageClient().getProgressService();
                    }

                    @Override
                    public CompletableFuture<SemanticTokens> getDocumentSemanticTokens(
                        SemanticTokensParams params)
                    {
                        Either<Boolean, SemanticTokensServerFull> full = options.getFull();
                        if (full == null
                            || !(full.isRight() || Boolean.TRUE.equals(full.getLeft())))
                            throw new UnsupportedOperationException();

                        return getLanguageServer().getTextDocumentService().semanticTokensFull(
                            params);
                    }

                    @Override
                    public CompletableFuture<
                        Either<SemanticTokens, SemanticTokensDelta>> getDocumentSemanticTokensDelta(
                            SemanticTokensDeltaParams params)
                    {
                        Either<Boolean, SemanticTokensServerFull> full = options.getFull();
                        if (full == null
                            || !(full.isRight() && Boolean.TRUE.equals(full.getRight().getDelta())))
                            throw new UnsupportedOperationException();

                        return getLanguageServer().getTextDocumentService().semanticTokensFullDelta(
                            params);
                    }

                    @Override
                    public CompletableFuture<SemanticTokens> getDocumentRangeSemanticTokens(
                        SemanticTokensRangeParams params)
                    {
                        Either<Boolean, Object> range = options.getRange();
                        if (range == null
                            || !(range.isRight() || Boolean.TRUE.equals(range.getLeft())))
                            throw new UnsupportedOperationException();

                        return getLanguageServer().getTextDocumentService().semanticTokensRange(
                            params);
                    }

                    @Override
                    public EventStream<Void> onRefreshSemanticTokens()
                    {
                        return onRefreshSemanticTokens;
                    }
                });
            rollback.add(registration::dispose);

            Disposable subscription = getLanguageClient().onRefreshSemanticTokens().subscribe(
                e -> onRefreshSemanticTokens.emit(e, logger));
            rollback.add(subscription::dispose);

            rollback.setLogger(logger);
            return rollback::run;
        });
    }
}
