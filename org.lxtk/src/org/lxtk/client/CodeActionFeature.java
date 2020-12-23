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
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.CodeActionCapabilities;
import org.eclipse.lsp4j.CodeActionOptions;
import org.eclipse.lsp4j.CodeActionParams;
import org.eclipse.lsp4j.Command;
import org.eclipse.lsp4j.DocumentFilter;
import org.eclipse.lsp4j.Registration;
import org.eclipse.lsp4j.ServerCapabilities;
import org.eclipse.lsp4j.TextDocumentClientCapabilities;
import org.eclipse.lsp4j.TextDocumentRegistrationOptions;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.lxtk.CodeActionProvider;
import org.lxtk.CommandService;
import org.lxtk.LanguageService;
import org.lxtk.util.Disposable;

/**
 * Participates in a given {@link LanguageService} by implementing and
 * dynamically contributing {@link CodeActionProvider}s according to LSP.
 * <p>
 * This implementation is thread-safe.
 * </p>
 */
public final class CodeActionFeature
    extends TextDocumentLanguageFeature<TextDocumentRegistrationOptions>
{
    private static final String METHOD = "textDocument/codeAction"; //$NON-NLS-1$
    private static final Set<String> METHODS = Collections.singleton(METHOD);

    private final CommandService commandService;

    /**
     * Constructor.
     *
     * @param languageService not <code>null</code>
     * @param commandService not <code>null</code>
     */
    public CodeActionFeature(LanguageService languageService, CommandService commandService)
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
        CodeActionCapabilities codeAction = getLanguageService().getCodeActionCapabilities();
        codeAction.setDynamicRegistration(true);
        capabilities.setCodeAction(codeAction);
    }

    @Override
    void initialize(ServerCapabilities capabilities, List<DocumentFilter> documentSelector)
    {
        if (documentSelector == null)
            return;

        Either<Boolean, CodeActionOptions> capability = capabilities.getCodeActionProvider();
        if (capability == null
            || !(capability.isRight() || Boolean.TRUE.equals(capability.getLeft())))
            return;

        register(new Registration(UUID.randomUUID().toString(), METHOD,
            new TextDocumentRegistrationOptions(documentSelector)));
    }

    @Override
    Class<TextDocumentRegistrationOptions> getRegistrationOptionsClass()
    {
        return TextDocumentRegistrationOptions.class;
    }

    @Override
    Disposable registerLanguageFeatureProvider(String method,
        TextDocumentRegistrationOptions options)
    {
        return getLanguageService().getCodeActionProviders().add(new CodeActionProvider()
        {
            @Override
            public TextDocumentRegistrationOptions getRegistrationOptions()
            {
                return options;
            }

            @Override
            public CompletableFuture<List<Either<Command, CodeAction>>> getCodeActions(
                CodeActionParams params)
            {
                return getLanguageServer().getTextDocumentService().codeAction(params);
            }

            @Override
            public CommandService getCommandService()
            {
                return commandService;
            }
        });
    }
}
