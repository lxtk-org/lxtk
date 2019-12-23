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

import java.util.List;

import org.eclipse.lsp4j.ClientCapabilities;
import org.eclipse.lsp4j.DocumentFilter;
import org.eclipse.lsp4j.InitializeParams;
import org.eclipse.lsp4j.ServerCapabilities;
import org.eclipse.lsp4j.services.LanguageServer;
import org.lxtk.util.Disposable;

/**
 * TODO JavaDoc
 *
 * @param <S> server interface type
 */
public interface Feature<S extends LanguageServer>
    extends Disposable
{
    /**
     * Fills the relevant initialize params.
     *
     * @param params not <code>null</code>
     */
    default void fillInitializeParams(InitializeParams params)
    {
    }

    /**
     * Fills the client capabilities this feature implements.
     *
     * @param capabilities not <code>null</code>
     */
    void fillClientCapabilities(ClientCapabilities capabilities);

    /**
     * Initializes this feature. Called just after the client sent the
     * initialized notification to the server.
     *
     * @param server not <code>null</code>
     * @param capabilities not <code>null</code>
     * @param documentSelector a default document selector, or <code>null</code>
     */
    void initialize(S server, ServerCapabilities capabilities,
        List<DocumentFilter> documentSelector);
}