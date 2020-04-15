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

import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionList;
import org.eclipse.lsp4j.CompletionOptions;
import org.eclipse.lsp4j.CompletionParams;
import org.eclipse.lsp4j.CompletionRegistrationOptions;
import org.eclipse.lsp4j.DocumentFilter;
import org.eclipse.lsp4j.Registration;
import org.eclipse.lsp4j.ServerCapabilities;
import org.eclipse.lsp4j.TextDocumentClientCapabilities;
import org.eclipse.lsp4j.TextDocumentRegistrationOptions;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.lxtk.CompletionProvider;
import org.lxtk.LanguageService;
import org.lxtk.util.Disposable;

/**
 * Participates in a given {@link LanguageService} by implementing and
 * dynamically contributing {@link CompletionProvider}s according to LSP.
 * <p>
 * This implementation is thread-safe.
 * </p>
 */
public final class CompletionFeature
    extends LanguageFeature
{
    private static final String METHOD = "textDocument/completion"; //$NON-NLS-1$
    private static final Set<String> METHODS = Collections.singleton(METHOD);

    /**
     * Constructor.
     *
     * @param languageService not <code>null</code>
     */
    public CompletionFeature(LanguageService languageService)
    {
        super(languageService);
    }

    @Override
    public Set<String> getMethods()
    {
        return METHODS;
    }

    @Override
    protected void fillClientCapabilities(
        TextDocumentClientCapabilities capabilities)
    {
        capabilities.setCompletion(
            getLanguageService().getCompletionCapabilities());
    }

    @Override
    protected void initialize(ServerCapabilities capabilities,
        List<DocumentFilter> documentSelector)
    {
        if (documentSelector == null)
            return;

        CompletionOptions capability = capabilities.getCompletionProvider();
        if (capability == null)
            return;

        CompletionRegistrationOptions registerOptions =
            new CompletionRegistrationOptions(capability.getTriggerCharacters(),
                capability.getResolveProvider());
        registerOptions.setDocumentSelector(documentSelector);

        register(new Registration(UUID.randomUUID().toString(), METHOD,
            registerOptions));
    }

    @Override
    protected Disposable registerLanguageFeatureProvider(String method,
        TextDocumentRegistrationOptions options)
    {
        CompletionRegistrationOptions castedOptions =
            (CompletionRegistrationOptions)options;
        return getLanguageService().getCompletionProviders().add(
            new CompletionProvider()
            {
                @Override
                public CompletionRegistrationOptions getRegistrationOptions()
                {
                    return castedOptions;
                }

                @Override
                public CompletableFuture<Either<List<CompletionItem>, CompletionList>> getCompletionItems(
                    CompletionParams params)
                {
                    return getLanguageServer().getTextDocumentService().completion(
                        params);
                }

                @Override
                public CompletableFuture<CompletionItem> resolveCompletionItem(
                    CompletionItem item)
                {
                    if (!Boolean.TRUE.equals(
                        castedOptions.getResolveProvider()))
                        throw new UnsupportedOperationException();

                    return getLanguageServer().getTextDocumentService().resolveCompletionItem(
                        item);
                }
            });
    }
}
