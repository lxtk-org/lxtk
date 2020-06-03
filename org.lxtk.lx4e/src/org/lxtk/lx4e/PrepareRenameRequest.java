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
package org.lxtk.lx4e;

import java.text.MessageFormat;
import java.util.concurrent.Future;

import org.eclipse.lsp4j.PrepareRenameResult;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextDocumentPositionParams;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.lxtk.RenameProvider;

/**
 * A request for preparing rename operation.
 */
public class PrepareRenameRequest
    extends LanguageFeatureRequest<RenameProvider, TextDocumentPositionParams,
        Either<Range, PrepareRenameResult>>
{
    @Override
    protected Future<Either<Range, PrepareRenameResult>> send(RenameProvider provider,
        TextDocumentPositionParams params)
    {
        setTitle(MessageFormat.format(Messages.PrepareRenameRequest_title, params));
        return provider.prepareRename(params);
    }
}
