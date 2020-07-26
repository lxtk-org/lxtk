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

import org.eclipse.lsp4j.ImplementationParams;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.LocationLink;
import org.eclipse.lsp4j.StaticRegistrationOptions;
import org.eclipse.lsp4j.jsonrpc.messages.Either;

/**
 * Provides implementation locations for the symbol denoted by a given text document position.
 *
 * @see LanguageService
 */
public interface ImplementationProvider
    extends LanguageFeatureProvider
{
    @Override
    StaticRegistrationOptions getRegistrationOptions();

    /**
     * Requests implementation locations for the symbol denoted by the given text document position.
     *
     * @param params not <code>null</code>
     * @return result future (never <code>null</code>)
     */
    CompletableFuture<
        Either<List<? extends Location>, List<? extends LocationLink>>> getImplementation(
            ImplementationParams params);
}
