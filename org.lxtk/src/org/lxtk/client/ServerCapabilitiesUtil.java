/*******************************************************************************
 * Copyright (c) 2019 1C-Soft LLC.
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
    /**
     * TODO JavaDoc
     *
     * @param capabilities not <code>null</code>
     * @return text document sync kind (never <code>null</code>)
     */
    public static TextDocumentSyncKind getTextDocumentSyncKind(
        ServerCapabilities capabilities)
    {
        Either<TextDocumentSyncKind, TextDocumentSyncOptions> either =
            capabilities.getTextDocumentSync();
        if (either == null)
            return TextDocumentSyncKind.None;
        if (either.isLeft())
            return either.getLeft();
        TextDocumentSyncKind change = either.getRight().getChange();
        if (change == null)
            return TextDocumentSyncKind.None;
        return change;
    }

    private ServerCapabilitiesUtil()
    {
    }
}
