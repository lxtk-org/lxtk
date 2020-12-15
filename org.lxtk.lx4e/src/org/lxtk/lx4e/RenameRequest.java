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
import java.util.concurrent.CompletableFuture;

import org.eclipse.lsp4j.RenameParams;
import org.eclipse.lsp4j.WorkspaceEdit;
import org.lxtk.RenameProvider;

/**
 * Requests the workspace edit for the given {@link RenameParams}.
 */
public class RenameRequest
    extends LanguageFeatureRequest<RenameProvider, RenameParams, WorkspaceEdit>
{
    @Override
    protected CompletableFuture<WorkspaceEdit> send(RenameProvider provider, RenameParams params)
    {
        setTitle(MessageFormat.format(Messages.RenameRequest_title, params));
        return provider.getRenameEdits(params);
    }
}
