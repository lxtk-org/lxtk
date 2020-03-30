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

import org.eclipse.lsp4j.DocumentFilter;
import org.eclipse.lsp4j.Registration;
import org.eclipse.lsp4j.ServerCapabilities;
import org.eclipse.lsp4j.SignatureHelp;
import org.eclipse.lsp4j.SignatureHelpOptions;
import org.eclipse.lsp4j.SignatureHelpRegistrationOptions;
import org.eclipse.lsp4j.TextDocumentClientCapabilities;
import org.eclipse.lsp4j.TextDocumentPositionParams;
import org.eclipse.lsp4j.TextDocumentRegistrationOptions;
import org.lxtk.LanguageService;
import org.lxtk.SignatureHelpProvider;
import org.lxtk.util.Disposable;

/**
 * TODO JavaDoc
 */
public final class SignatureHelpFeature
    extends LanguageFeature
{
    private static final String METHOD = "textDocument/signatureHelp"; //$NON-NLS-1$
    private static final Set<String> METHODS = Collections.singleton(METHOD);

    /**
     * TODO JavaDoc
     *
     * @param languageService not <code>null</code>
     */
    public SignatureHelpFeature(LanguageService languageService)
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
        capabilities.setSignatureHelp(
            getLanguageService().getSignatureHelpCapabilities());
    }

    @Override
    protected void initialize(ServerCapabilities capabilities,
        List<DocumentFilter> documentSelector)
    {
        if (documentSelector == null)
            return;

        SignatureHelpOptions capability =
            capabilities.getSignatureHelpProvider();
        if (capability == null)
            return;

        SignatureHelpRegistrationOptions registerOptions =
            new SignatureHelpRegistrationOptions(
                capability.getTriggerCharacters());
        registerOptions.setDocumentSelector(documentSelector);

        register(new Registration(UUID.randomUUID().toString(), METHOD,
            registerOptions));
    }

    @Override
    protected Disposable registerLanguageFeatureProvider(String method,
        TextDocumentRegistrationOptions options)
    {
        SignatureHelpRegistrationOptions castedOptions =
            (SignatureHelpRegistrationOptions)options;
        return getLanguageService().getSignatureHelpProviders().add(
            new SignatureHelpProvider()
            {
                @Override
                public SignatureHelpRegistrationOptions getRegistrationOptions()
                {
                    return castedOptions;
                }

                @Override
                public CompletableFuture<SignatureHelp> getSignatureHelp(
                    TextDocumentPositionParams params)
                {
                    return getLanguageServer().getTextDocumentService().signatureHelp(
                        params);
                }
            });
    }
}
