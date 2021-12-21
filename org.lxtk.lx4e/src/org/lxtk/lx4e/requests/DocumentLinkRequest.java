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
package org.lxtk.lx4e.requests;

import java.text.MessageFormat;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.eclipse.lsp4j.DocumentLink;
import org.eclipse.lsp4j.DocumentLinkParams;
import org.lxtk.DocumentLinkProvider;

/**
 * Requests document links for the given text document.
 */
public class DocumentLinkRequest
    extends LanguageFeatureRequestWithWorkDoneAndPartialResultProgress<DocumentLinkProvider,
        DocumentLinkParams, List<DocumentLink>>
{
    @Override
    protected CompletableFuture<List<DocumentLink>> send(
        DocumentLinkProvider provider, DocumentLinkParams params)
    {
        setTitle(MessageFormat.format(Messages.DocumentLinkRequest_title, params));
        return provider.getDocumentLinks(params);
    }
}
