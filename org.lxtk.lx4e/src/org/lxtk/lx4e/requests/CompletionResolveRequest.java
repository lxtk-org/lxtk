/*******************************************************************************
 * Copyright (c) 2020, 2021 1C-Soft LLC.
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

import org.eclipse.lsp4j.CompletionItem;
import org.lxtk.CompletionProvider;

/**
 * Requests additional information for the given completion item.
 */
public class CompletionResolveRequest
    extends LanguageFeatureRequest<CompletionProvider, CompletionItem, CompletionItem>
{
    @Override
    protected CompletableFuture<CompletionItem> send(CompletionProvider provider, CompletionItem item)
    {
        setTitle(MessageFormat.format(Messages.CompletionResolveRequest_title, item));
        return provider.resolveCompletionItem(item);
    }
}
