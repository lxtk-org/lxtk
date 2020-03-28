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
import org.lxtk.util.Registry;

/**
 * TODO JavaDoc
 */
public interface LanguageService
{
    /**
     * Returns a document matcher. The returned document matcher is functionally
     * identical to the one returned by {@link Workspace#getDocumentMatcher()}.
     *
     * @return a document matcher (never <code>null</code>)
     */
    default DocumentMatcher getDocumentMatcher()
    {
        return DefaultDocumentMatcher.INSTANCE;
    }

    /**
     * TODO JavaDoc
     *
     * @return code action capabilities (never <code>null</code>)
     */
    CodeActionCapabilities getCodeActionCapabilities();

    /**
     * TODO JavaDoc
     *
     * @return the registry for code action providers (never <code>null</code>)
     */
    Registry<CodeActionProvider> getCodeActionProviders();

    /**
     * TODO JavaDoc
     *
     * @return completion capabilities (never <code>null</code>)
     */
    CompletionCapabilities getCompletionCapabilities();

    /**
     * TODO JavaDoc
     *
     * @return the registry for completion providers (never <code>null</code>)
     */
    Registry<CompletionProvider> getCompletionProviders();

    /**
     * TODO JavaDoc
     *
     * @return definition capabilities (never <code>null</code>)
     */
    DefinitionCapabilities getDefinitionCapabilities();

    /**
     * TODO JavaDoc
     *
     * @return the registry for definition providers (never <code>null</code>)
     */
    Registry<DefinitionProvider> getDefinitionProviders();

    /**
     * TODO JavaDoc
     *
     * @return document highlight capabilities (never <code>null</code>)
     */
    DocumentHighlightCapabilities getDocumentHighlightCapabilities();

    /**
     * TODO JavaDoc
     *
     * @return the registry for document highlight providers (never <code>null</code>)
     */
    Registry<DocumentHighlightProvider> getDocumentHighlightProviders();

    /**
     * TODO JavaDoc
     *
     * @return document symbol capabilities (never <code>null</code>)
     */
    DocumentSymbolCapabilities getDocumentSymbolCapabilities();

    /**
     * TODO JavaDoc
     *
     * @return the registry for document symbol providers (never <code>null</code>)
     */
    Registry<DocumentSymbolProvider> getDocumentSymbolProviders();

    /**
     * TODO JavaDoc
     *
     * @return hover capabilities (never <code>null</code>)
     */
    HoverCapabilities getHoverCapabilities();

    /**
     * TODO JavaDoc
     *
     * @return the registry for hover providers (never <code>null</code>)
     */
    Registry<HoverProvider> getHoverProviders();

    /**
     * TODO JavaDoc
     *
     * @return references capabilities (never <code>null</code>)
     */
    ReferencesCapabilities getReferencesCapabilities();

    /**
     * TODO JavaDoc
     *
     * @return the registry for reference providers (never <code>null</code>)
     */
    Registry<ReferenceProvider> getReferenceProviders();

    /**
     * TODO JavaDoc
     *
     * @return rename capabilities (never <code>null</code>)
     */
    RenameCapabilities getRenameCapabilities();

    /**
     * TODO JavaDoc
     *
     * @return the registry for rename providers (never <code>null</code>)
     */
    Registry<RenameProvider> getRenameProviders();
}
