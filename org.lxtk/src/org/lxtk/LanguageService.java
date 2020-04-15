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
package org.lxtk;

import org.eclipse.lsp4j.CodeActionCapabilities;
import org.eclipse.lsp4j.CompletionCapabilities;
import org.eclipse.lsp4j.DefinitionCapabilities;
import org.eclipse.lsp4j.DocumentHighlightCapabilities;
import org.eclipse.lsp4j.DocumentSymbolCapabilities;
import org.eclipse.lsp4j.HoverCapabilities;
import org.eclipse.lsp4j.ReferencesCapabilities;
import org.eclipse.lsp4j.RenameCapabilities;
import org.eclipse.lsp4j.SignatureHelpCapabilities;
import org.lxtk.util.Registry;

/**
 * Provides support for participating in language-specific editing features,
 * like code completion, code actions, rename, etc.
 *
 * @see DefaultLanguageService
 */
public interface LanguageService
{
    /**
     * Returns the default document matcher for this service.
     *
     * @return the default document matcher (never <code>null</code>)
     */
    default DocumentMatcher getDocumentMatcher()
    {
        return DefaultDocumentMatcher.INSTANCE;
    }

    /**
     * Returns code action capabilities provided by this service.
     *
     * @return code action capabilities (never <code>null</code>).
     *  Clients <b>must not</b> modify the returned object
     */
    CodeActionCapabilities getCodeActionCapabilities();

    /**
     * Returns the registry of code action providers for this service.
     *
     * @return the registry of code action providers (never <code>null</code>)
     */
    Registry<CodeActionProvider> getCodeActionProviders();

    /**
     * Returns completion capabilities provided by this service.
     *
     * @return completion capabilities (never <code>null</code>).
     *  Clients <b>must not</b> modify the returned object
     */
    CompletionCapabilities getCompletionCapabilities();

    /**
     * Returns the registry of completion providers for this service.
     *
     * @return the registry of completion providers (never <code>null</code>)
     */
    Registry<CompletionProvider> getCompletionProviders();

    /**
     * Returns definition capabilities provided by this service.
     *
     * @return definition capabilities (never <code>null</code>).
     *  Clients <b>must not</b> modify the returned object
     */
    DefinitionCapabilities getDefinitionCapabilities();

    /**
     * Returns the registry of definition providers for this service.
     *
     * @return the registry of definition providers (never <code>null</code>)
     */
    Registry<DefinitionProvider> getDefinitionProviders();

    /**
     * Returns document highlight capabilities provided by this service.
     *
     * @return document highlight capabilities (never <code>null</code>).
     *  Clients <b>must not</b> modify the returned object
     */
    DocumentHighlightCapabilities getDocumentHighlightCapabilities();

    /**
     * Returns the registry of document highlight providers for this service.
     *
     * @return the registry of document highlight providers (never <code>null</code>)
     */
    Registry<DocumentHighlightProvider> getDocumentHighlightProviders();

    /**
     * Returns document symbol capabilities provided by this service.
     *
     * @return document symbol capabilities (never <code>null</code>).
     *  Clients <b>must not</b> modify the returned object
     */
    DocumentSymbolCapabilities getDocumentSymbolCapabilities();

    /**
     * Returns the registry of document symbol providers for this service.
     *
     * @return the registry of document symbol providers (never <code>null</code>)
     */
    Registry<DocumentSymbolProvider> getDocumentSymbolProviders();

    /**
     * Returns hover capabilities provided by this service.
     *
     * @return hover capabilities (never <code>null</code>).
     *  Clients <b>must not</b> modify the returned object
     */
    HoverCapabilities getHoverCapabilities();

    /**
     * Returns the registry of hover providers for this service.
     *
     * @return the registry of hover providers (never <code>null</code>)
     */
    Registry<HoverProvider> getHoverProviders();

    /**
     * Returns references capabilities provided by this service.
     *
     * @return references capabilities (never <code>null</code>).
     *  Clients <b>must not</b> modify the returned object
     */
    ReferencesCapabilities getReferencesCapabilities();

    /**
     * Returns the registry of reference providers for this service.
     *
     * @return the registry of reference providers (never <code>null</code>)
     */
    Registry<ReferenceProvider> getReferenceProviders();

    /**
     * Returns rename capabilities provided by this service.
     *
     * @return rename capabilities (never <code>null</code>).
     *  Clients <b>must not</b> modify the returned object
     */
    RenameCapabilities getRenameCapabilities();

    /**
     * Returns the registry of rename providers for this service.
     *
     * @return the registry of rename providers (never <code>null</code>)
     */
    Registry<RenameProvider> getRenameProviders();

    /**
     * Returns signature help capabilities provided by this service.
     *
     * @return signature help capabilities (never <code>null</code>).
     *  Clients <b>must not</b> modify the returned object
     */
    SignatureHelpCapabilities getSignatureHelpCapabilities();

    /**
     * Returns the registry of signature help providers for this service.
     *
     * @return the registry of signature help providers (never <code>null</code>)
     */
    Registry<SignatureHelpProvider> getSignatureHelpProviders();
}
