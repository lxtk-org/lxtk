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
package org.lxtk;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.eclipse.lsp4j.TypeHierarchyItem;
import org.eclipse.lsp4j.TypeHierarchyPrepareParams;
import org.eclipse.lsp4j.TypeHierarchyRegistrationOptions;
import org.eclipse.lsp4j.TypeHierarchySubtypesParams;
import org.eclipse.lsp4j.TypeHierarchySupertypesParams;

/**
 * Provides a type hierarchy for the symbol at a given text document position.
 *
 * @see LanguageService
 */
public interface TypeHierarchyProvider
    extends LanguageFeatureProvider<TypeHierarchyRegistrationOptions>
{
    /**
     * Requests preparation of a type hierarchy for the symbol at the given text document position.
     *
     * @param params not <code>null</code>
     * @return result future (never <code>null</code>)
     */
    CompletableFuture<List<TypeHierarchyItem>> prepareTypeHierarchy(
        TypeHierarchyPrepareParams params);

    /**
     * Requests the super-types for the given type hierarchy item.
     *
     * @param params not <code>null</code>
     * @return result future (never <code>null</code>)
     */
    CompletableFuture<List<TypeHierarchyItem>> getTypeHierarchySupertypes(
        TypeHierarchySupertypesParams params);

    /**
     * Requests the sub-types for the given type hierarchy item.
     *
     * @param params not <code>null</code>
     * @return result future (never <code>null</code>)
     */
    CompletableFuture<List<TypeHierarchyItem>> getTypeHierarchySubtypes(
        TypeHierarchySubtypesParams params);
}
