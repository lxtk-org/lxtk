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
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.eclipse.lsp4j.DocumentFilter;
import org.eclipse.lsp4j.PrepareRenameResult;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.Registration;
import org.eclipse.lsp4j.RenameOptions;
import org.eclipse.lsp4j.RenameParams;
import org.eclipse.lsp4j.ServerCapabilities;
import org.eclipse.lsp4j.TextDocumentClientCapabilities;
import org.eclipse.lsp4j.TextDocumentPositionParams;
import org.eclipse.lsp4j.WorkspaceEdit;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.lxtk.LanguageService;
import org.lxtk.RenameProvider;
import org.lxtk.util.Disposable;

/**
 * Participates in a given {@link LanguageService} by implementing and
 * dynamically contributing {@link RenameProvider}s according to LSP.
 * <p>
 * This implementation is thread-safe.
 * </p>
 */
public class RenameFeature
    extends LanguageFeature<RenameOptions>
{
    private static final String METHOD = "textDocument/rename"; //$NON-NLS-1$
    private static final Set<String> METHODS = Collections.singleton(METHOD);

    /**
     * Constructor.
     *
     * @param languageService not <code>null</code>
     */
    public RenameFeature(LanguageService languageService)
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
        capabilities.setRename(getLanguageService().getRenameCapabilities());
    }

    @Override
    protected void initialize(ServerCapabilities capabilities,
        List<DocumentFilter> documentSelector)
    {
        if (documentSelector == null)
            return;

        Either<Boolean, RenameOptions> capability =
            capabilities.getRenameProvider();
        if (capability == null || !(capability.isRight() || Boolean.TRUE.equals(
            capability.getLeft())))
            return;

        RenameOptions registerOptions = new RenameOptions();
        RenameOptions options = capability.getRight();
        if (options == null)
        {
            registerOptions.setDocumentSelector(documentSelector);
            registerOptions.setId(UUID.randomUUID().toString());
            registerOptions.setPrepareProvider(false);
        }
        else
        {
            registerOptions.setDocumentSelector(Optional.ofNullable(
                options.getDocumentSelector()).orElse(documentSelector));
            registerOptions.setId(Optional.ofNullable(options.getId()).orElse(
                UUID.randomUUID().toString()));
            registerOptions.setPrepareProvider(options.getPrepareProvider());
        }

        register(new Registration(registerOptions.getId(), METHOD,
            registerOptions));
    }

    @Override
    protected Class<RenameOptions> getRegistrationOptionsClass()
    {
        return RenameOptions.class;
    }

    @Override
    protected Disposable registerLanguageFeatureProvider(String method,
        RenameOptions options)
    {
        return getLanguageService().getRenameProviders().add(
            new RenameProvider()
            {
                @Override
                public RenameOptions getRegistrationOptions()
                {
                    return options;
                }

                @Override
                public CompletableFuture<WorkspaceEdit> getRenameEdits(
                    RenameParams params)
                {
                    return getLanguageServer().getTextDocumentService().rename(
                        params);
                }

                @Override
                public CompletableFuture<Either<Range, PrepareRenameResult>> prepareRename(
                    TextDocumentPositionParams params)
                {
                    if (!Boolean.TRUE.equals(options.getPrepareProvider()))
                        throw new UnsupportedOperationException();

                    return getLanguageServer().getTextDocumentService().prepareRename(
                        params);
                }
            });
    }
}
