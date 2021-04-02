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
package org.lxtk.lx4e;

import java.text.MessageFormat;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.CodeActionParams;
import org.eclipse.lsp4j.Command;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.lxtk.CodeActionProvider;

/**
 * Requests code actions for the given {@link CodeActionParams}.
 */
public class CodeActionRequest
    extends LanguageFeatureRequestWithWorkDoneAndPartialResultProgress<CodeActionProvider,
        CodeActionParams, List<Either<Command, CodeAction>>>
{
    @Override
    protected CompletableFuture<List<Either<Command, CodeAction>>> send(CodeActionProvider provider,
        CodeActionParams params)
    {
        setTitle(MessageFormat.format(Messages.CodeActionRequest_title, params));
        return provider.getCodeActions(params);
    }
}
