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
package org.lxtk.jsonrpc;

import org.eclipse.lsp4j.jsonrpc.Endpoint;
import org.lxtk.util.connect.Connection;

/**
 * Represents a JSON-RPC connection.
 *
 * @param <T> remote interface type
 */
public interface JsonRpcConnection<T>
    extends Connection
{
    /**
     * Returns a proxy instance that implements the remote service interface
     * for this connection.
     *
     * @return a proxy instance that implements the remote service interface
     *  (never <code>null</code>)
     */
    T getRemoteProxy();

    /**
     * Returns the remote endpoint for this connection.
     *
     * @return the remote endpoint (never <code>null</code>)
     */
    Endpoint getRemoteEndpoint();
}
