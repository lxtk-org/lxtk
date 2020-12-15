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

import java.util.List;

import org.eclipse.lsp4j.ClientCapabilities;
import org.eclipse.lsp4j.DocumentFilter;
import org.eclipse.lsp4j.InitializeParams;
import org.eclipse.lsp4j.InitializeResult;
import org.eclipse.lsp4j.jsonrpc.Endpoint;
import org.eclipse.lsp4j.services.LanguageServer;
import org.lxtk.util.Disposable;

/**
 * Represents a feature of the language client.
 *
 * @param <S> server interface type
 * @see DynamicFeature
 * @see AbstractLanguageClient
 */
public interface Feature<S extends LanguageServer>
    extends Disposable
{
    /**
     * Sets the owning language client of this feature.
     *
     * @param client not <code>null</code>
     */
    default void setLanguageClient(AbstractLanguageClient<? extends S> client)
    {
    }

    /**
     * Fills the relevant initialize params.
     *
     * @param params the initialize params to fill (not <code>null</code>)
     */
    default void fillInitializeParams(InitializeParams params)
    {
    }

    /**
     * Fills the client capabilities this feature implements.
     *
     * @param capabilities the client capabilities to fill (not <code>null</code>)
     */
    void fillClientCapabilities(ClientCapabilities capabilities);

    /**
     * Allows this feature to advise the server endpoint.
     *
     * @param endpoint the server endpoint (not <code>null</code>)
     * @return the advised endpoint (never <code>null</code>)
     */
    default Endpoint adviseServerEndpoint(Endpoint endpoint)
    {
        return endpoint;
    }

    /**
     * Initializes this feature. Called just after the client sends the
     * 'initialized' notification to the server.
     *
     * @param server the server proxy (not <code>null</code>)
     * @param initializeResult the server initialize result (not <code>null</code>)
     * @param documentSelector the default document selector, or <code>null</code>
     */
    void initialize(S server, InitializeResult initializeResult,
        List<DocumentFilter> documentSelector);
}
