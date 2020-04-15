/*******************************************************************************
 * Copyright (c) 2019, 2020 1C-Soft LLC.
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

import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.CodeActionParams;
import org.eclipse.lsp4j.Command;
import org.eclipse.lsp4j.jsonrpc.messages.Either;

/**
 * Provides {@link CodeAction}s for a given text document range.
 *
 * @see LanguageService
 */
public interface CodeActionProvider
    extends LanguageFeatureProvider
{
    // TODO CodeActionRegistrationOptions is missing in LSP4J
    //CodeActionRegistrationOptions getRegistrationOptions();

    /**
     * Computes code actions for the given {@link CodeActionParams}.
     *
     * @param params not <code>null</code>
     * @return result future (never <code>null</code>)
     */
    CompletableFuture<List<Either<Command, CodeAction>>> getCodeActions(
        CodeActionParams params);
}
