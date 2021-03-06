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
package org.lxtk.client;

import org.eclipse.lsp4j.ServerCapabilities;
import org.eclipse.lsp4j.TextDocumentSyncKind;
import org.eclipse.lsp4j.TextDocumentSyncOptions;
import org.eclipse.lsp4j.jsonrpc.messages.Either;

class ServerCapabilitiesUtil
{
    static TextDocumentSyncOptions getTextDocumentSyncOptions(ServerCapabilities capabilities)
    {
        Either<TextDocumentSyncKind, TextDocumentSyncOptions> either =
            capabilities.getTextDocumentSync();
        if (either == null)
            return new TextDocumentSyncOptions();

        if (either.isRight())
            return either.getRight();

        TextDocumentSyncOptions options = new TextDocumentSyncOptions();
        TextDocumentSyncKind syncKind = either.getLeft();
        options.setChange(syncKind);
        if (syncKind != null && syncKind != TextDocumentSyncKind.None)
            options.setOpenClose(true);
        return options;
    }

    private ServerCapabilitiesUtil()
    {
    }
}
