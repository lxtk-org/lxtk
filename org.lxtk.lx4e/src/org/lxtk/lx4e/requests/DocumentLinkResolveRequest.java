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
import java.util.concurrent.CompletableFuture;

import org.eclipse.lsp4j.DocumentLink;
import org.lxtk.DocumentLinkProvider;

/**
 * Resolves the given document link.
 */
public class DocumentLinkResolveRequest
    extends LanguageFeatureRequest<DocumentLinkProvider, DocumentLink, DocumentLink>
{
    @Override
    protected CompletableFuture<DocumentLink> send(DocumentLinkProvider provider, DocumentLink link)
    {
        setTitle(MessageFormat.format(Messages.DocumentLinkResolveRequest_title, link));
        return provider.resolveDocumentLink(link);
    }
}
