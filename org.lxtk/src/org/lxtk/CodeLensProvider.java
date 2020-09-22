/*******************************************************************************
 * Copyright (c) 2020 1C-Soft LLC.
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

import org.eclipse.lsp4j.CodeLens;
import org.eclipse.lsp4j.CodeLensParams;
import org.eclipse.lsp4j.CodeLensRegistrationOptions;

/**
 * Provides {@link CodeLens}es for a given text document.
 *
 * @see LanguageService
 */
public interface CodeLensProvider
    extends LanguageFeatureProvider<CodeLensRegistrationOptions>
{
    /**
     * Requests code lenses for the given text document.
     *
     * @param params not <code>null</code>
     * @return result future (never <code>null</code>)
     */
    CompletableFuture<List<? extends CodeLens>> getCodeLenses(CodeLensParams params);

    /**
     * Resolves the given unresolved code lens.
     *
     * @param unresolved not <code>null</code>
     * @return result future (never <code>null</code>)
     * @throws UnsupportedOperationException if no support for code lens resolving is available
     * @see CodeLensRegistrationOptions#getResolveProvider()
     */
    CompletableFuture<CodeLens> resolveCodeLens(CodeLens unresolved);

    /**
     * Returns the command service associated with this provider.
     *
     * @return the associated command service (never <code>null</code>)
     */
    CommandService getCommandService();
}
