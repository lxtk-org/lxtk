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
package org.lxtk;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.eclipse.lsp4j.DocumentLink;
import org.eclipse.lsp4j.DocumentLinkParams;
import org.eclipse.lsp4j.DocumentLinkRegistrationOptions;

/**
 * Provides document links for a given text document.
 *
 * @see LanguageService
 */
public interface DocumentLinkProvider
    extends LanguageFeatureProvider<DocumentLinkRegistrationOptions>
{
    /**
     * Requests document links for the given text document.
     *
     * @param params not <code>null</code>
     * @return result future (never <code>null</code>)
     */
    CompletableFuture<List<DocumentLink>> getDocumentLinks(DocumentLinkParams params);

    /**
     * Resolves the given unresolved document link.
     *
     * @param unresolved not <code>null</code>
     * @return result future (never <code>null</code>)
     * @throws UnsupportedOperationException if no support for document link resolving is available
     * @see DocumentLinkRegistrationOptions#getResolveProvider()
     */
    CompletableFuture<DocumentLink> resolveDocumentLink(DocumentLink unresolved);
}
