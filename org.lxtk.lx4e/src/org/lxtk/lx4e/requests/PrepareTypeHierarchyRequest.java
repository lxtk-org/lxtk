/*******************************************************************************
 * Copyright (c) 2022 1C-Soft LLC.
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

import org.eclipse.lsp4j.TypeHierarchyItem;
import org.eclipse.lsp4j.TypeHierarchyPrepareParams;
import org.lxtk.TypeHierarchyProvider;

/**
 * Requests preparation of a type hierarchy for the symbol at the given text document position.
 */
public class PrepareTypeHierarchyRequest
    extends LanguageFeatureRequestWithWorkDoneProgress<TypeHierarchyProvider,
        TypeHierarchyPrepareParams, List<TypeHierarchyItem>>
{
    @Override
    protected CompletableFuture<List<TypeHierarchyItem>> send(TypeHierarchyProvider provider,
        TypeHierarchyPrepareParams params)
    {
        setTitle(MessageFormat.format(Messages.PrepareTypeHierarchyRequest_title, params));
        return provider.prepareTypeHierarchy(params);
    }
}
