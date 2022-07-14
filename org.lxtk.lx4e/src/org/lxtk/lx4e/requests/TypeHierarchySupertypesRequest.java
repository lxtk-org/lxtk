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
import org.eclipse.lsp4j.TypeHierarchySupertypesParams;
import org.lxtk.TypeHierarchyProvider;

/**
 * Requests the super-types for the given type hierarchy item.
 */
public class TypeHierarchySupertypesRequest
    extends LanguageFeatureRequestWithWorkDoneAndPartialResultProgress<TypeHierarchyProvider,
        TypeHierarchySupertypesParams, List<TypeHierarchyItem>>
{
    @Override
    protected CompletableFuture<List<TypeHierarchyItem>> send(TypeHierarchyProvider provider,
        TypeHierarchySupertypesParams params)
    {
        setTitle(MessageFormat.format(Messages.TypeHierarchySupertypesRequest_title, params));
        return provider.getTypeHierarchySupertypes(params);
    }
}
