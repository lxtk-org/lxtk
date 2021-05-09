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
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.LocationLink;
import org.eclipse.lsp4j.TypeDefinitionParams;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.lxtk.TypeDefinitionProvider;

/**
 * Requests type definition locations for the symbol denoted by the given text document position.
 */
public class TypeDefinitionRequest
    extends LanguageFeatureRequestWithWorkDoneAndPartialResultProgress<TypeDefinitionProvider,
        TypeDefinitionParams, Either<List<? extends Location>, List<? extends LocationLink>>>
{
    @Override
    protected CompletableFuture<
        Either<List<? extends Location>, List<? extends LocationLink>>> send(
            TypeDefinitionProvider provider, TypeDefinitionParams params)
    {
        setTitle(MessageFormat.format(Messages.TypeDefinitionRequest_title, params));
        return provider.getTypeDefinition(params);
    }
}
