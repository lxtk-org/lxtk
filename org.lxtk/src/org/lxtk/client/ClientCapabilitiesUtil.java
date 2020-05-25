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

import java.util.Optional;

import org.eclipse.lsp4j.ClientCapabilities;
import org.eclipse.lsp4j.SynchronizationCapabilities;
import org.eclipse.lsp4j.TextDocumentClientCapabilities;
import org.eclipse.lsp4j.WorkspaceClientCapabilities;

class ClientCapabilitiesUtil
{
    static TextDocumentClientCapabilities getOrCreateTextDocument(ClientCapabilities capabilities)
    {
        return Optional.ofNullable(capabilities.getTextDocument()).orElseGet(() ->
        {
            TextDocumentClientCapabilities textDocument = new TextDocumentClientCapabilities();
            capabilities.setTextDocument(textDocument);
            return textDocument;
        });
    }

    static SynchronizationCapabilities getOrCreateSynchronization(
        TextDocumentClientCapabilities capabilities)
    {
        return Optional.ofNullable(capabilities.getSynchronization()).orElseGet(() ->
        {
            SynchronizationCapabilities synchronization = new SynchronizationCapabilities();
            capabilities.setSynchronization(synchronization);
            return synchronization;
        });
    }

    static WorkspaceClientCapabilities getOrCreateWorkspace(ClientCapabilities capabilities)
    {
        return Optional.ofNullable(capabilities.getWorkspace()).orElseGet(() ->
        {
            WorkspaceClientCapabilities workspace = new WorkspaceClientCapabilities();
            capabilities.setWorkspace(workspace);
            return workspace;
        });
    }

    private ClientCapabilitiesUtil()
    {
    }
}
