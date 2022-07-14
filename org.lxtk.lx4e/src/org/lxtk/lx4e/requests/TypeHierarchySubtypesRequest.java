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
import org.eclipse.lsp4j.TypeHierarchySubtypesParams;
import org.lxtk.TypeHierarchyProvider;

/**
 * Requests the sub-types for the given type hierarchy item.
 */
public class TypeHierarchySubtypesRequest
    extends LanguageFeatureRequestWithWorkDoneAndPartialResultProgress<TypeHierarchyProvider,
        TypeHierarchySubtypesParams, List<TypeHierarchyItem>>
{
    @Override
    protected CompletableFuture<List<TypeHierarchyItem>> send(TypeHierarchyProvider provider,
        TypeHierarchySubtypesParams params)
    {
        setTitle(MessageFormat.format(Messages.TypeHierarchySubtypesRequest_title, params));
        return provider.getTypeHierarchySubtypes(params);
    }
}
