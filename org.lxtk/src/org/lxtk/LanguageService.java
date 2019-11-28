/*******************************************************************************
 * Copyright (c) 2019 1C-Soft LLC.
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

import org.eclipse.lsp4j.CompletionCapabilities;
import org.eclipse.lsp4j.DefinitionCapabilities;
import org.eclipse.lsp4j.DocumentSymbolCapabilities;
import org.eclipse.lsp4j.ReferencesCapabilities;
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
     * @return references capabilities (never <code>null</code>)
     */
    ReferencesCapabilities getReferencesCapabilities();

    /**
     * TODO JavaDoc
     *
     * @return the registry for reference providers (never <code>null</code>)
     */
    Registry<ReferenceProvider> getReferenceProviders();
}
